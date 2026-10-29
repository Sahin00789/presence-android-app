# On-Device Biometric Face Pipeline & Anti-Spoofing Policy (`docs/FACE_VERIFICATION.md`)

## 1. Multi-Pose (3D-Style) Biometric System Architecture

Biometric processing runs on the Android device using ONNX Runtime Mobile with XNNPACK optimization. The system extracts **5 distinct 128-dimensional pose vectors** during multi-pose enrollment (`CENTER`, `LEFT`, `RIGHT`, `UP`, `DOWN`) and matches live clock-in vectors against all 5 stored vectors in PostgreSQL using `pgvector` HNSW cosine distance indexing (`<=>` operator).

```
+-----------------------------------------------------------------------------------+
|                        Android Client Multi-Pose Pipeline                        |
+-------------------+--------------------+-------------------+----------------------+
| 1. Frame Capture  | 2. YuNet ONNX      | 3. Liveness Check | 4. SFace ONNX        |
|    CameraX API    | Detection & 5-pt   | Active (Blink)    | Recognition 128-dim  |
|  Live Video Feed  | Facial Landmarks   | Passive (Laplace) | Normalized Embedding |
+-------------------+--------------------+-------------------+----------------------+
                                           |
                                           v
                          +----------------------------------+
                          | 5. Retrofit Biometrics Network   |
                          | POST /api/v1/biometrics/enroll   |
                          | POST /api/v1/biometrics/clock-in |
                          +----------------------------------+
                                           |
                                           v
                          +----------------------------------+
                          | 6. pgvector HNSW Cosine Distance |
                          |  LEAST(center_vec <=> live, ...) |
                          |  Fastify + PostgreSQL (HNSW)     |
                          +----------------------------------+
```

---

## 2. On-Device Models & Anti-Spoof Liveness Pipeline

### AI Models & Runtime Setup
- **YuNet Face Detector (`yunet.onnx`)**: Detects face bounding boxes and extracts 5 key facial landmarks.
- **SFace Recognizer (`sface.onnx`)**: Aligns face crop using similarity transform to $112\times112$ and outputs a $128$-element `FloatArray` embedding.
- **XNNPACK Acceleration**: Both ONNX sessions execute with `OptLevel.ALL_OPT`, 2 intra-op threads, and XNNPACK CPU provider options for sub-50ms inference times.

---

### Pose Estimation Geometry

Pose orientation is computed dynamically using relative 2D landmark geometry:

| Pose | Landmark Condition |
| :--- | :--- |
| **`CENTER`** | Nose $X$ centered relative to eye midpoint ($\pm 10\%$). |
| **`LEFT`** | Head turned left (nose shifts right relative to eyes $X$). |
| **`RIGHT`** | Head turned right (nose shifts left relative to eyes $X$). |
| **`UP`** | Head tilted up (nose shifts upwards towards eye level). |
| **`DOWN`** | Head tilted down (nose shifts downwards towards mouth level). |

---

### Anti-Spoofing Liveness Checks

Before auto-capturing any pose vector, two anti-spoof checks must pass:

1. **Active Liveness (Eye-Blink Tracker)**: Tracks Eye Aspect Ratio (EAR) across frame sequences.
2. **Passive Liveness (Laplacian Texture Variance)**: Computes Laplacian texture variance $\sigma^2_L$ over grayscale face region ($\sigma^2_L \ge 15.0$).

---

## 3. Backend Database & pgvector Cosine Distance Query

### Database Table: `multi_pose_face_templates`

```sql
CREATE TABLE multi_pose_face_templates (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
  employee_id UUID NOT NULL REFERENCES employees(id) ON DELETE CASCADE UNIQUE,
  center_vector vector(128) NOT NULL,
  left_vector vector(128) NOT NULL,
  right_vector vector(128) NOT NULL,
  up_vector vector(128) NOT NULL,
  down_vector vector(128) NOT NULL,
  quality_score NUMERIC(4,3) DEFAULT 0.950,
  enrolled_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- HNSW Cosine Distance Indexes for Sub-Millisecond Search
CREATE INDEX idx_hnsw_center_vec ON multi_pose_face_templates USING hnsw (center_vector vector_cosine_ops);
CREATE INDEX idx_hnsw_left_vec ON multi_pose_face_templates USING hnsw (left_vector vector_cosine_ops);
CREATE INDEX idx_hnsw_right_vec ON multi_pose_face_templates USING hnsw (right_vector vector_cosine_ops);
CREATE INDEX idx_hnsw_up_vec ON multi_pose_face_templates USING hnsw (up_vector vector_cosine_ops);
CREATE INDEX idx_hnsw_down_vec ON multi_pose_face_templates USING hnsw (down_vector vector_cosine_ops);
```

---

### Authoritative Cosine Distance Matching

During live clock-in, Fastify compares the single live vector $E_{\text{live}}$ against all 5 stored pose vectors in parallel:

```sql
SELECT
  employee_id,
  LEAST(
    center_vector <=> $1::vector(128),
    left_vector <=> $1::vector(128),
    right_vector <=> $1::vector(128),
    up_vector <=> $1::vector(128),
    down_vector <=> $1::vector(128)
  )::float AS min_distance
FROM multi_pose_face_templates
WHERE organization_id = $2
ORDER BY min_distance ASC
LIMIT 1;
```

$$\text{Similarity Score} = 1.0 - \text{min\_distance}$$

| Similarity Score | Result | Action |
| :--- | :--- | :--- |
| $\ge 0.64$ ($\text{dist} \le 0.36$) | **VERIFIED MATCH** | Clock-in accepted for matched `employee_id`. |
| $< 0.64$ ($\text{dist} > 0.36$) | **REJECTED MATCH** | Verification failed. Attendance exception created. |

---

## 4. API Specification

### `POST /api/v1/biometrics/enroll`
Saves 5 distinct 128-float pose vectors for an employee.

- **Request Payload**:
  ```json
  {
    "employeeId": "c39e258f-1234-4567-8901-abcdef123456",
    "centerVector": [0.012, -0.045, ... 128 floats],
    "leftVector": [0.018, -0.039, ... 128 floats],
    "rightVector": [0.011, -0.048, ... 128 floats],
    "upVector": [0.015, -0.042, ... 128 floats],
    "downVector": [0.014, -0.044, ... 128 floats],
    "qualityScore": 0.98
  }
  ```

### `POST /api/v1/biometrics/clock-in`
Validates a single live 128-float vector against all stored pose columns using `pgvector`.
