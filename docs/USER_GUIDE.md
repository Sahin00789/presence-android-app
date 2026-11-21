# Presence — User & Operational Guide

> Complete Operational Manual for Business Owners, Managers & Staff
> **Registration • Daily Clock-In • Shifts • Overtime • Leaves • Earning Ledger & Payroll**

---

## 1. Getting Started & Account Registration

Presence connects business owners and workforce employees into a unified workspace. Access and onboarding depend on your role inside the organization.

### 1.1 Owner & Organization Registration
- **Business Owner Self-Registration**: Owners sign up directly in the app using Email 6-Digit OTP or Google Single Sign-On (SSO).
- **Workspace Creation**: Set your business organization name, operating currency (INR ₹), and timezone.
- **Primary Workplace (Premises)**: Define your main workplace name, street address, GPS location coordinates, and geofence radius (default **50 metres**).

### 1.2 Employee Onboarding & Invitation
- **Direct Additions by Owner**: Owners directly add staff members by specifying their full name, email address, phone number, role (`EMPLOYEE`), department, designation, and assigned premises.
- **Employee Sign-In Rule**: Unregistered employees cannot self-register. When signing in with Email OTP or Google SSO, the system verifies whether an invitation exists. If not registered, access is refused with:
  > *"You are not registered. Please contact your Administrator to receive an invitation."*

---

## 2. Daily Attendance & Biometric Verification Logics

### 2.1 Clock-In Factors (Face Verification + Geofence)
To clock in for a shift or overtime session, two concurrent verification checks must pass on your mobile device:

| Verification Factor | How It Works | Verification Requirement |
| :--- | :--- | :--- |
| **GPS Geofence** | Measures geodesic distance between current device GPS reading and workplace coordinates. | Must be within workplace radius (**50m default**). |
| **Face Biometrics** | Captures live camera feed and extracts a 128-dimensional numerical face embedding. | Must match enrolled multi-pose face template on-device. |

> [!NOTE]
> Face verification is processed entirely on-device using CameraX and ONNX Runtime Mobile. Raw facial photos are never transmitted or stored on backend servers; only 128-dimensional numerical vector embeddings are stored securely.

### 2.2 Active-Shift Periodic Location Checks
While a shift or overtime session is active, Presence runs a WorkManager background location check approximately **every 10 minutes** to ensure staff remain within workplace premises.

> [!IMPORTANT]
> **Active-Shift Location Monitoring & Default Present Policy**
> - **3+ Outside Pings Alert**: If an employee records 3 consecutive location checks outside the workplace geofence, an attendance exception alert is raised for the Owner.
> - **Owner Manual Action**: On the Live Monitor tab, the Owner can manually set attendance to **ABSENT**, **HALF_DAY**, **LEAVE**, or **RESUME** tracking. Action buttons remain active until the shift/overtime session completes.
> - **Default Present Policy**: If the Owner takes **NO manual action** before the shift or overtime completes, the attendance status **automatically defaults to Present (`PRESENT`)**.

---

## 3. Shifts, Overtime & Leave Management

### 3.1 Shift Templates & Roster Assignments
- **Shift Templates**: Owners create shift definitions specifying timing windows (e.g., 09:00 AM – 05:00 PM), assigned weekdays (Everyday, Mon–Fri, Weekdays, Weekends), and half-day thresholds.
- **Pay Rates**: Assigning an employee to a shift configures custom pay rates for Present days (`present_pay`), Half-days (`half_day_pay`), Approved Leaves (`leave_pay`), and Absent penalties (`absent_pay`).
- **Auto Weekly Offs**: Days of the week not assigned to a shift automatically become the employee's Weekly Off days.

### 3.2 Pre-Approved Overtime
Owners raise pre-approved overtime sessions specifying the staff member, work date, start/end time, and agreed overtime payment amount (in ₹). Employees can then clock in for the overtime session.

### 3.3 Leave Request Workflow
Employees view their 14-day upcoming schedule and apply for leave on specific shift dates. Owners review requests on their dashboard and approve or reject them with remarks. Approving a leave updates attendance and accrues leave pay automatically.

---

## 4. Earning Ledger & Monthly Payroll Engine

Presence maintains an itemized append-only earning ledger for every employee. All monetary calculations are performed in **exact whole integer units in Indian Rupees (INR ₹)** with zero decimal floating-point rounding errors.

| Earning Type | Trigger Event | Payment Rule |
| :--- | :--- | :--- |
| **`SHIFT`** | Shift Clock-Out or Approved Paid Leave | Accrues shift `present_pay`, `half_day_pay`, or `leave_pay`. |
| **`HOLIDAY`** | Unworked Paid Holiday | Accrues holiday payment rule amount (or daily rate). |
| **`OFF_DAY`** | Unworked Weekly Off Day | Accrues weekly off payment rule amount (or daily rate). |
| **`OVERTIME`** | Completed Overtime Session | Accrues approved overtime payment amount. |

### 4.1 Monthly Payroll Reconciliation & Payslips
- **Automated Reconciliation**: Monthly payroll totals automatically calculate `Total Earned Balance` minus `Total Payments Disbursed` to show remaining `Due Balance`.
- **Recording Payments**: Owners disburse full or partial payments specifying payment date, method (Cash, Bank Transfer, UPI), and reference number.
- **PDF Payslips**: Staff and Owners can view itemized monthly ledger breakdowns and download official PDF payslips.

---

## 5. Real-World Scenarios & System Behaviors

| # | Operational Scenario | How Presence Handles It |
| :--- | :--- | :--- |
| **1** | **Employee Clocking In at Workplace** | Employee opens app at workplace and taps Clock In. App checks GPS coordinates against workplace geofence (within 50m) and matches camera face scan against enrolled face template. When verified, clock-in is recorded as `PRESENT`. |
| **2** | **Working an Overnight Shift** | For overnight shifts (e.g., 10:00 PM to 06:00 AM), an employee clocking in after midnight (e.g., 01:00 AM) is automatically attributed back to yesterday's shift start date. Earnings and records map accurately to the shift work date. |
| **3** | **Going Outside Geofence During Shift** | Background service checks location every ~10 mins. If 3 consecutive checks are outside, an alert appears on the Owner's Live Monitor. The Owner can override attendance (`ABSENT`, `HALF_DAY`, `LEAVE`). If no action is taken before shift ends, status defaults to `PRESENT`. |
| **4** | **Working on a Holiday or Off-Day** | An unworked paid holiday or weekly off accrues base rule pay. If the employee comes in and works a shift on a holiday or off-day, it is recorded as a worked shift and earns full shift pay. |
| **5** | **Applying for Leave** | Employee selects shift dates from their 14-day schedule and submits a leave request. Owner receives an in-app notification to Approve or Reject. Approval automatically sets attendance to `LEAVE` and credits leave pay. |
| **6** | **Booking & Working Overtime** | Owner creates a pre-approved overtime session for a staff member. On the scheduled date, employee taps Clock In for Overtime. Upon clock-out, overtime duration is completed and overtime pay is credited to their ledger. |
| **7** | **Monthly Salary Disbursement** | Owner opens Payroll screen, views total monthly earnings and due balance for each staff member, taps Pay Staff, records payment details (UPI/Bank/Cash), and system issues a downloadable PDF payslip. |
| **8** | **Signing In on a New Phone** | Presence permits exactly **1 active device session** per user. Signing in on a new phone automatically invalidates the session on the old phone, ensuring session security. |

---

## 6. Screen-by-Screen User Interface Guide

### 6.1 Owner Dashboard Tabs & Features
- **Live Monitor Tab (`LiveMonitorTab.kt`)**: Displays real-time presence cards for all staff on duty today, Inside/Outside geofence indicators, recent location check pings, exception badges, and manual resolution buttons (`ABSENT`, `HALF_DAY`, `LEAVE`, `RESUME`).
- **Workforce Roster (`EmployeesManagementScreen.kt`)**: Roster of all employees across workplace premises. Shows cumulative earned ledger balance, paid disbursements, and due amount. Features "Assign Shift" button launching `AssignShiftModal.kt`.
- **Shift Manager (`ShiftManagementScreen.kt`)**: Create and edit shift templates, timing windows, weekday assignment patterns, and half-day thresholds.
- **Approvals & Overtime (`OwnerLeaveScreen.kt`, `OwnerOvertimeScreen.kt`)**: Review employee leave requests with approve/reject remarks. Book pre-approved overtime sessions specifying staff member, work date, time, and payment.
- **Payroll & Disbursements (`PayrollScreen.kt`, `PayStaffModal.kt`)**: Reconcile monthly staff payrolls, record payments (Cash/Bank/UPI), view daily itemized earning ledgers, and export PDF/CSV reports.

### 6.2 Employee Dashboard Tabs & Features
- **Clock-In Scanner (`EmployeeClockInTab.kt`)**: Live camera scanner performing face matching and GPS geofence validation for active shift or overtime check-in.
- **My Attendance (`MyAttendanceScreen.kt`)**: Calendar and list view of daily attendance records, shift start/end timestamps, worked duration, and late minutes.
- **My Schedule & Leave (`MyShiftsScreen.kt`, `EmployeeLeaveScreen.kt`)**: View 14-day schedule projection (assigned shifts, holidays, weekly offs) and submit leave applications.
- **Earning Ledger & Payslips (`EmployeeLedgerTab.kt`)**: Track daily itemized earned breakdown (`SHIFT`, `HOLIDAY`, `OFF_DAY`, `OVERTIME`) and download official monthly payslip PDF statements.
