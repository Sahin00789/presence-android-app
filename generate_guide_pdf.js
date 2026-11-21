import PDFDocument from './server/node_modules/pdfkit/js/pdfkit.js';
import fs from 'fs';
import path from 'path';

const doc = new PDFDocument({
  margin: 40,
  size: 'A4',
  bufferPages: true,
});

let outputPath = path.join(process.cwd(), 'Presence_System_Guide.pdf');
try {
  fs.unlinkSync(outputPath);
} catch (_e) {
  outputPath = path.join(process.cwd(), 'Presence_System_Guide_v2.pdf');
}
const stream = fs.createWriteStream(outputPath);
doc.pipe(stream);

// Color Palette
const PRIMARY = '#0F172A';        // Dark Slate 900
const SECONDARY = '#0284C7';      // Sky 600
const ACCENT = '#0D9488';         // Teal 600
const TEXT_DARK = '#0F172A';      // Dark Slate Body
const TEXT_BODY = '#334155';      // Body Slate 700
const BG_BOX = '#F0F9FF';         // Sky 50
const BORDER_BOX = '#BAE6FD';     // Sky 200
const ROW_ALT = '#F8FAFC';        // Table Alt Row

function checkSpace(neededHeight = 35) {
  if (doc.y + neededHeight > 790) {
    doc.addPage();
  }
}

function drawTitleHeader(title, subtitle) {
  doc.rect(0, 0, 595, 115).fill(PRIMARY);
  doc.fillColor('#FFFFFF').fontSize(26).font('Helvetica-Bold').text(title, 40, 25);
  doc.fontSize(11.5).font('Helvetica').fillColor('#E2E8F0').text(subtitle, 40, 60);
  doc.fontSize(9).fillColor('#38BDF8').text('Technical Architecture, Business Logics, Workflows & System Manual | v1.0', 40, 82);
  doc.y = 130;
}

function drawSectionHeader(title) {
  checkSpace(45);
  doc.moveDown(0.2);
  const startY = doc.y;
  doc.rect(40, startY, 515, 22).fill(PRIMARY);
  doc.fillColor('#FFFFFF').fontSize(12).font('Helvetica-Bold').text(title, 46, startY + 5);
  doc.y = startY + 28;
}

function drawSubSectionHeader(title) {
  checkSpace(25);
  doc.moveDown(0.15);
  doc.fillColor(SECONDARY).fontSize(11).font('Helvetica-Bold').text(title);
  doc.moveDown(0.1);
}

function drawParagraph(text) {
  checkSpace(25);
  doc.fillColor(TEXT_BODY).fontSize(9.5).font('Helvetica').text(text, { align: 'justify', lineGap: 2.5 });
  doc.moveDown(0.25);
}

function drawBullet(title, text) {
  checkSpace(20);
  doc.fillColor(TEXT_DARK).fontSize(9.5).font('Helvetica-Bold').text(`• ${title}: `, { continued: true });
  doc.fillColor(TEXT_BODY).font('Helvetica').text(text, { align: 'justify', lineGap: 2.5 });
  doc.moveDown(0.2);
}

function drawFlowStep(num, title, desc) {
  checkSpace(30);
  const startY = doc.y;
  doc.rect(40, startY, 515, 22).fillAndStroke('#F1F5F9', '#CBD5E1');
  doc.fillColor(SECONDARY).fontSize(9.5).font('Helvetica-Bold').text(`STEP ${num}: ${title}`, 46, startY + 5, { continued: true });
  doc.fillColor(TEXT_BODY).font('Helvetica').text(`  —  ${desc}`, { lineGap: 1.5 });
  doc.y = startY + 26;
}

function drawCallout(title, text) {
  checkSpace(45);
  const boxWidth = 515;
  doc.fontSize(9).font('Helvetica');
  const textHeight = doc.heightOfString(text, { width: boxWidth - 16 });
  const totalHeight = textHeight + 20;

  if (doc.y + totalHeight > 790) {
    doc.addPage();
  }

  const currentY = doc.y;
  doc.rect(40, currentY, boxWidth, totalHeight).fillAndStroke(BG_BOX, BORDER_BOX);
  doc.fillColor('#0369A1').fontSize(10).font('Helvetica-Bold').text(title, 48, currentY + 6);
  doc.fillColor(TEXT_BODY).fontSize(9).font('Helvetica').text(text, 48, currentY + 18, { width: boxWidth - 16, lineGap: 2 });

  doc.y = currentY + totalHeight + 8;
}

function drawTable(headers, rows) {
  checkSpace(50);
  const startY = doc.y;
  const colWidth = 515 / headers.length;

  // Header Row
  doc.rect(40, startY, 515, 20).fill(SECONDARY);
  headers.forEach((h, i) => {
    doc.fillColor('#FFFFFF').fontSize(9).font('Helvetica-Bold').text(h, 44 + i * colWidth, startY + 5, { width: colWidth - 8 });
  });

  let currentY = startY + 20;
  rows.forEach((row, rowIndex) => {
    doc.fontSize(8.5).font('Helvetica');
    const maxHeight = Math.max(...row.map(cell => doc.heightOfString(String(cell), { width: colWidth - 8 }))) + 8;

    if (currentY + maxHeight > 790) {
      doc.addPage();
      currentY = doc.y;
    }

    if (rowIndex % 2 === 1) {
      doc.rect(40, currentY, 515, maxHeight).fill(ROW_ALT);
    }

    row.forEach((cell, i) => {
      doc.fillColor(TEXT_DARK).fontSize(8.5).font('Helvetica').text(String(cell), 44 + i * colWidth, currentY + 4, { width: colWidth - 8 });
    });

    doc.rect(40, currentY, 515, maxHeight).stroke('#E2E8F0');
    currentY += maxHeight;
  });

  doc.y = currentY + 8;
}

// ================= BUILD DOCUMENT =================

drawTitleHeader('PRESENCE SYSTEM MANUAL', 'Employee Attendance, Shift Management & Salary Ledger System');

// --- SECTION 1 ---
drawSectionHeader('1. End-to-End System Architecture & Stack Mechanics');

drawParagraph(
  'Presence is an enterprise workforce platform designed for multi-tenant organizations. It combines an Android client (Jetpack Compose, CameraX, ONNX Runtime Mobile, Fused Location, Room, WorkManager) communicating with a Node.js Fastify 5 + PostgreSQL 16 backend service layer over REST APIs and real-time WebSockets.'
);

drawSubSectionHeader('1.1 Component Data Flow Steps');
drawFlowStep(1, 'Android UI Layer', 'Compose Material 3 UI observes ViewModels via StateFlow. Captures live CameraX frames and FusedLocation GPS readings.');
drawFlowStep(2, 'Repository & Caching', 'UseCases execute REST API requests via Retrofit 2 / OkHttp 4 while maintaining offline fallbacks in local Room DB.');
drawFlowStep(3, 'Backend Router & Auth', 'Fastify 5 validates Zod request schemas and checks JWT access claims (15-minute expiry).');
drawFlowStep(4, 'Service & Financial Core', 'Service layer executes business rules, emits real-time WebSocket domain events, and commits to PostgreSQL via Drizzle ORM.');

drawSubSectionHeader('1.2 Core Architectural Guarantees');
drawBullet('Multi-Tenant Isolation', 'Every entity belongs to an organization_id. Owners possess administrative scope across all premises; Employees are scoped strictly to their assigned premises_id.');
drawBullet('Single-Device Concurrency', 'Exactly 1 active session is permitted per user. Logging in on a new device automatically revokes all prior active device sessions in logged_devices.');
drawBullet('Integer Financial Math', 'All monetary values operate strictly in whole integer units (INR ₹ / Paise). Floating-point math for money is strictly prohibited across backend and client.');

// --- SECTION 2 ---
drawSectionHeader('2. Authentication & Session Concurrency Logics');

drawParagraph(
  'Presence supports Email 6-digit OTP verification and Google OAuth 2.0 Single Sign-On (ID token & server authorization code exchange). Active device sessions are tracked in PostgreSQL logged_devices with SHA-256 hashed refresh tokens.'
);

drawSubSectionHeader('2.1 Authentication Process Steps');
drawFlowStep(1, 'Auth Request', 'User submits Email OTP or Google OAuth credential along with device metadata (ipAddress, userAgent, deviceName).');
drawFlowStep(2, 'Access Verification', 'Backend verifies user status. Unregistered employees are rejected (403 "Contact Administrator"); unregistered owners are directed to Registration.');
drawFlowStep(3, 'Single-Device Invalidation', 'In a transaction, all prior active logged_devices rows for the user are marked inactive (revokedReason: SINGLE_DEVICE_CONCURRENCY).');
drawFlowStep(4, 'Session Issuance', 'Generates access JWT (15-min expiry), rotating refresh token (90-day expiry), stores refresh_token_hash, and returns workspace profile.');

drawSubSectionHeader('2.2 Role & Permission Matrix');
drawTable(
  ['Capability / Feature', 'Owner Role (OWNER)', 'Employee Role (EMPLOYEE)'],
  [
    ['Data Table', 'organizations.owner_user_id', 'employees (role = EMPLOYEE)'],
    ['Scope', 'Organization-Wide (All Premises)', 'Assigned Premises (premises_id)'],
    ['Workforce Management', 'Add, Edit, Deactivate Employees', 'View Self Profile (/employees/me)'],
    ['Shifts & Pay Rates', 'Define Templates & Set Pay Rates', 'View 14-Day Assigned Schedule'],
    ['Attendance & Pings', 'Live Monitor & Override Alerts', 'Clock-In/Out via Face + GPS'],
    ['Leave & Overtime', 'Approve Leaves & Book Overtime', 'Submit Leave Applications'],
    ['Payroll & Ledger', 'Disburse Payments & Lock Payroll', 'View Daily Earning Ledger & Payslips']
  ]
);

// --- SECTION 3 ---
drawSectionHeader('3. Attendance, Geofencing & Biometrics Verification Engine');

drawParagraph(
  'Clock-in verification requires two concurrent factors: GPS geofence validation and local 128D ONNX Runtime face embedding matching against enrolled multi-pose templates.'
);

drawSubSectionHeader('3.1 Clock-In Pipeline Steps');
drawFlowStep(1, 'Device Sampling', 'CameraX captures camera feed, ONNX Runtime Mobile extracts 128D face vector; FusedLocation reads GPS coordinates.');
drawFlowStep(2, 'Geofence Evaluation', 'Evaluates geodesic Haversine distance against premises coordinates. Distance must be within premises radius (default 50 metres).');
drawFlowStep(3, 'Face Vector Matching', 'Server pgvector HNSW cosine distance (<=>) matches live vector against enrolled multi-pose templates (CENTER, LEFT, RIGHT, UP, DOWN) >= 0.75 threshold.');
drawFlowStep(4, 'Overnight Shift Attribution', 'Clock-ins after midnight for overnight shifts map back to yesterday\'s shift start date.');
drawFlowStep(5, 'Attendance Persistence', 'Inserts employee_shift_attendance row (Status: PRESENT) and emits real-time WebSocket attendance_update event.');

drawCallout(
  'ACTIVE-SHIFT GPS MONITORING & DEFAULT PRESENT POLICY',
  '• Periodic Location Pings: ShiftLocationService (Foreground Service) checks position every ~10 minutes during active shifts.\n• 3+ Consecutive Outside Pings (outside_count > 3): Automatically raises an attendance exception alert for Owner review.\n• Manual Action Buttons: Owner can manually set status to ABSENT, HALF_DAY, LEAVE, or RESUME tracking.\n• Default Policy: If Owner takes NO action before shift/overtime completes, status automatically defaults to Present (PRESENT).'
);

// --- SECTION 4 ---
drawSectionHeader('4. Shifts, Rosters & Financial Earning Ledger');

drawSubSectionHeader('4.1 Shift Templates & Weekday Assignments');
drawParagraph(
  'Shifts define shiftName, dayName (Everyday, Mon-Fri, Weekdays, Weekends, custom range), startTime, endTime, and halfDayThreshold. Shift assignments link employees to templates with custom pay rates (present_pay, half_day_pay, leave_pay, absent_pay) and automatically calculate weekly off days.'
);

drawSubSectionHeader('4.2 Append-Only Earning Ledger Breakdown');
drawTable(
  ['Earning Ledger Type', 'Trigger Event', 'Pay Calculation Rule'],
  [
    ['SHIFT', 'Shift Clock-Out or Approved Paid Leave', 'Uses shift assignment present_pay, half_day_pay, or leave_pay'],
    ['HOLIDAY', 'Unworked Paid Holiday', 'Uses unworked_payment_rules multiplier or fixed payment_inr'],
    ['OFF_DAY', 'Unworked Weekly Off Day', 'Uses unworked_payment_rules multiplier or fixed payment_inr'],
    ['OVERTIME', 'Completed Overtime Session', 'Uses approved overtime payment_inr amount']
  ]
);

drawSubSectionHeader('4.3 Financial Ledger & Payroll Reconciliation Steps');
drawFlowStep(1, 'Work Event Completion', 'Shift clock-out, approved paid leave, paid holiday, weekly off day, or completed overtime triggers ledger accrual.');
drawFlowStep(2, 'Append Ledger Entry', 'Inserts row into staff_earning_ledger with type (SHIFT, HOLIDAY, OFF_DAY, OVERTIME) and idempotent sourceReference.');
drawFlowStep(3, 'Monthly Reconciliation', 'Reconciles SUM(staff_earning_ledger.amount) vs SUM(payroll_transactions.amount) to calculate due_amount.');
drawFlowStep(4, 'Disbursement & Payslips', 'Owner records disbursement via PayStaffModal; system updates payroll status and issues official PDF payslip.');

// --- SECTION 5 ---
drawSectionHeader('5. Application User Guide & Screen-by-Screen Walkthrough');

drawSubSectionHeader('5.1 Owner Workspace Guide');
drawBullet('Live Monitor Tab (LiveMonitorTab.kt)', 'Real-time staff presence cards, Inside/Outside indicators, recent location pings, exception badges, and manual attendance override actions (ABSENT, HALF_DAY, LEAVE, RESUME).');
drawBullet('Workforce Roster (EmployeesManagementScreen.kt)', 'Lists employees across premises, displays accumulated earned balances, paid amounts, and due balances. Features "Assign Shift" launching AssignShiftWithRulesModal.kt.');
drawBullet('Shift Manager (ShiftManagementScreen.kt)', 'Create and edit shift templates, set timing windows, weekday patterns, and half-day thresholds.');
drawBullet('Approvals & Overtime (OwnerLeaveScreen.kt, OwnerOvertimeScreen.kt)', 'Review staff leave requests with remarks; create pre-approved overtime sessions specifying target employee, work date, and payment amount.');
drawBullet('Payroll & Disbursements (PayrollScreen.kt, PayStaffModal.kt)', 'Reconcile monthly payroll summaries, record salary payments, view daily itemized earning ledgers, and export PDF/CSV reports.');

drawSubSectionHeader('5.2 Employee Workspace Guide');
drawBullet('Clock-In Scanner (EmployeeClockInTab.kt)', 'CameraX scanner with live ONNX face vector matching and GPS geofence validation for active shift/overtime check-in.');
drawBullet('My Attendance (MyAttendanceScreen.kt)', 'Calendar and list view of attendance records, shift start/end timestamps, worked minutes, late minutes, and exception flags.');
drawBullet('My Schedule & Leave (MyShiftsScreen.kt, EmployeeLeaveScreen.kt)', '14-day schedule projection (shifts, holidays, weekly offs) and leave request submission.');
drawBullet('Earning Ledger & Payslips (EmployeeLedgerTab.kt)', 'Track daily itemized earned breakdown and download official monthly payslip PDF statements.');

// ================= FOOTER =================
const pageCount = doc.bufferedPageRange().count;
for (let i = 0; i < pageCount; i++) {
  doc.switchToPage(i);
  doc.fillColor('#94A3B8').fontSize(8.5).text(
    `Presence System Logic & User Guide | Page ${i + 1} of ${pageCount}`,
    40,
    815,
    { align: 'center', width: 515 }
  );
}

doc.end();

stream.on('finish', () => {
  console.log(`PDF guide successfully generated at: ${outputPath}`);
});
