import os
import sys

from reportlab.lib.pagesizes import A4
from reportlab.lib import colors
from reportlab.platypus import (
    SimpleDocTemplate, Paragraph, Spacer, Table, TableStyle, KeepTogether, HRFlowable
)
from reportlab.lib.styles import getSampleStyleSheet, ParagraphStyle
from reportlab.pdfgen import canvas

# --- STEP 1: REPORTLAB CANVAS WITH DYNAMIC PAGE NUMBERS ---

class NumberedCanvas(canvas.Canvas):
    def __init__(self, *args, **kwargs):
        super().__init__(*args, **kwargs)
        self._saved_page_states = []

    def showPage(self):
        self._saved_page_states.append(dict(self.__dict__))
        self._startPage()

    def save(self):
        num_pages = len(self._saved_page_states)
        for state in self._saved_page_states:
            self.__dict__.update(state)
            self.draw_page_decorations(num_pages)
            super().showPage()
        super().save()

    def draw_page_decorations(self, page_count):
        self.saveState()

        # Header on pages 2+
        if self._pageNumber > 1:
            self.setFillColor(colors.HexColor('#0F172A'))
            self.rect(0, 812, 595, 30, fill=True, stroke=False)
            self.setFillColor(colors.white)
            self.setFont("Helvetica-Bold", 10)
            self.drawString(40, 822, "PRESENCE — USER & OPERATIONAL GUIDE")
            self.setFont("Helvetica", 9)
            self.setFillColor(colors.HexColor('#38BDF8'))
            self.drawRightString(555, 822, "Business Owner & Employee Guide")

        # Footer on all pages
        self.setStrokeColor(colors.HexColor('#E2E8F0'))
        self.setLineWidth(0.8)
        self.line(40, 38, 555, 38)

        self.setFillColor(colors.HexColor('#64748B'))
        self.setFont("Helvetica", 8.5)
        self.drawString(40, 24, "Presence Staff & Attendance System | End-User Operational Manual")
        self.drawRightString(555, 24, f"Page {self._pageNumber} of {page_count}")

        self.restoreState()

# --- STEP 2: STYLES & SETUP ---

pdf_output_name = 'Presence_User_Guide.pdf'

doc = SimpleDocTemplate(
    pdf_output_name,
    pagesize=A4,
    leftMargin=40,
    rightMargin=40,
    topMargin=50,
    bottomMargin=50
)

styles = getSampleStyleSheet()

# Premium Color Palette
NAVY = colors.HexColor('#0F172A')
BLUE = colors.HexColor('#0284C7')
EMERALD = colors.HexColor('#059669')
AMBER = colors.HexColor('#D97706')
TEXT_DARK = colors.HexColor('#0F172A')
TEXT_BODY = colors.HexColor('#334155')
BG_LIGHT = colors.HexColor('#F8FAFC')
BG_CALLOUT = colors.HexColor('#F0F9FF')
BORDER_CALLOUT = colors.HexColor('#BAE6FD')

# Custom Typography Styles
style_title = ParagraphStyle('DocTitle', parent=styles['Normal'], fontName='Helvetica-Bold', fontSize=24, leading=28, textColor=colors.HexColor('#0F172A'))
style_subtitle = ParagraphStyle('DocSub', parent=styles['Normal'], fontName='Helvetica', fontSize=12, leading=15, textColor=colors.HexColor('#0284C7'))
style_badge = ParagraphStyle('Badge', parent=styles['Normal'], fontName='Helvetica-Bold', fontSize=9.5, leading=12, textColor=colors.HexColor('#059669'))

style_h1 = ParagraphStyle('H1', parent=styles['Normal'], fontName='Helvetica-Bold', fontSize=14, leading=17, textColor=NAVY, spaceBefore=14, spaceAfter=6)
style_h2 = ParagraphStyle('H2', parent=styles['Normal'], fontName='Helvetica-Bold', fontSize=11.5, leading=14, textColor=BLUE, spaceBefore=10, spaceAfter=4)

style_body = ParagraphStyle('BodyText', parent=styles['Normal'], fontName='Helvetica', fontSize=9.5, leading=13.5, textColor=TEXT_BODY, spaceAfter=6)
style_bullet = ParagraphStyle('BulletText', parent=styles['Normal'], fontName='Helvetica', fontSize=9.5, leading=13.5, textColor=TEXT_BODY, spaceAfter=4)

style_callout_title = ParagraphStyle('CalloutTitle', parent=styles['Normal'], fontName='Helvetica-Bold', fontSize=10, leading=13, textColor=colors.HexColor('#0369A1'))
style_callout_body = ParagraphStyle('CalloutBody', parent=styles['Normal'], fontName='Helvetica', fontSize=9, leading=12.5, textColor=TEXT_BODY)

style_table_head = ParagraphStyle('THead', parent=styles['Normal'], fontName='Helvetica-Bold', fontSize=9, leading=11.5, textColor=colors.white)
style_table_cell = ParagraphStyle('TCell', parent=styles['Normal'], fontName='Helvetica', fontSize=8.5, leading=11.5, textColor=TEXT_BODY)
style_table_cell_bold = ParagraphStyle('TCellBold', parent=styles['Normal'], fontName='Helvetica-Bold', fontSize=8.5, leading=11.5, textColor=TEXT_DARK)

story = []

# --- COVER / HEADER BANNER ---
banner_data = [[
    Paragraph("PRESENCE USER GUIDE", style_title),
], [
    Paragraph("Complete Operational Manual for Business Owners, Managers & Staff", style_subtitle)
], [
    Paragraph("Registration • Daily Clock-In • Shifts • Overtime • Leaves • Earning Ledger & Payroll", style_badge)
]]

banner_table = Table(banner_data, colWidths=[515])
banner_table.setStyle(TableStyle([
    ('BACKGROUND', (0,0), (-1,-1), colors.HexColor('#F8FAFC')),
    ('BOX', (0,0), (-1,-1), 1.5, colors.HexColor('#0284C7')),
    ('PADDING', (0,0), (-1,-1), 10),
    ('BOTTOMPADDING', (0,-1), (-1,-1), 10),
]))
story.append(banner_table)
story.append(Spacer(1, 12))

# ================= SECTION 1: GETTING STARTED & REGISTRATION =================
story.append(Paragraph("1. Getting Started & Account Registration", style_h1))
story.append(HRFlowable(width="100%", thickness=1.5, color=BLUE, spaceAfter=8))

story.append(Paragraph(
    "Presence connects business owners and workforce employees into a unified workspace. Access and onboarding depend on your role inside the organization.",
    style_body
))

story.append(Paragraph("1.1 Owner & Organization Registration", style_h2))
story.append(Paragraph("<b>• Business Owner Self-Registration:</b> Owners sign up directly in the app using Email OTP or Google Single Sign-On.", style_bullet))
story.append(Paragraph("<b>• Workspace Creation:</b> Set your business organization name, operating currency (INR ₹), and timezone.", style_bullet))
story.append(Paragraph("<b>• Primary Workplace (Premises):</b> Define your main workplace name, street address, GPS location coordinates, and geofence radius (default 50 metres).", style_bullet))

story.append(Paragraph("1.2 Employee Onboarding & Invitation", style_h2))
story.append(Paragraph("<b>• Direct Additions by Owner:</b> Owners directly add staff members by specifying their full name, email address, phone number, role, department, designation, and assigned premises.", style_bullet))
story.append(Paragraph("<b>• Employee Sign-In Rule:</b> Unregistered employees cannot self-register. When signing in with Email OTP or Google SSO, the system checks if an invitation exists. If not registered, access is refused with: <i>'You are not registered. Please contact your Administrator to receive an invitation.'</i>", style_bullet))

story.append(Spacer(1, 6))

# ================= SECTION 2: DAY-TO-DAY OPERATIONAL LOGICS =================
story.append(Paragraph("2. Daily Attendance & Biometric Verification Logics", style_h1))
story.append(HRFlowable(width="100%", thickness=1.5, color=BLUE, spaceAfter=8))

story.append(Paragraph("2.1 Clock-In Factors (Face Verification + Geofence)", style_h2))
story.append(Paragraph(
    "To clock in for a shift or overtime session, two concurrent verification checks must pass on your mobile device:",
    style_body
))

clockin_factors = [
    [Paragraph("<b>Verification Factor</b>", style_table_head), Paragraph("<b>How It Works</b>", style_table_head), Paragraph("<b>Verification Requirement</b>", style_table_head)],
    [Paragraph("<b>GPS Geofence</b>", style_table_cell_bold), Paragraph("Measures distance between current device GPS and workplace coordinates.", style_table_cell), Paragraph("Must be within workplace radius (50m default).", style_table_cell)],
    [Paragraph("<b>Face Biometrics</b>", style_table_cell_bold), Paragraph("Captures live camera feed and extracts a 128-dimensional face embedding.", style_table_cell), Paragraph("Must match enrolled multi-pose face template.", style_table_cell)]
]
t_factors = Table(clockin_factors, colWidths=[110, 215, 190])
t_factors.setStyle(TableStyle([
    ('BACKGROUND', (0,0), (-1,0), BLUE),
    ('BOX', (0,0), (-1,-1), 1, colors.HexColor('#BAE6FD')),
    ('INNERGRID', (0,0), (-1,-1), 0.5, colors.HexColor('#E2E8F0')),
    ('PADDING', (0,0), (-1,-1), 5),
    ('ROWBACKGROUNDS', (0,1), (-1,-1), [colors.white, BG_LIGHT])
]))
story.append(t_factors)

story.append(Paragraph("2.2 Active-Shift Periodic Location Checks", style_h2))
story.append(Paragraph(
    "While a shift or overtime session is active, Presence runs a background location check approximately every 10 minutes to ensure staff remain within workplace premises.",
    style_body
))

gps_policy_data = [[
    Paragraph("ACTIVE-SHIFT LOCATION MONITORING & DEFAULT PRESENT POLICY", style_callout_title)
], [
    Paragraph("<b>• 3+ Outside Pings Alert:</b> If an employee records 3 consecutive location checks outside the workplace, an attendance exception alert is raised for the Owner.<br/><b>• Owner Manual Action:</b> On the Live Monitor tab, the Owner can manually set attendance to <b>ABSENT</b>, <b>HALF_DAY</b>, <b>LEAVE</b>, or <b>RESUME</b> tracking.<br/><b>• Default Present Policy:</b> If the Owner takes <b>NO manual action</b> before the shift or overtime completes, the attendance status <b>automatically defaults to Present (PRESENT)</b>.", style_callout_body)
]]
t_gps_policy = Table(gps_policy_data, colWidths=[515])
t_gps_policy.setStyle(TableStyle([
    ('BACKGROUND', (0,0), (-1,-1), BG_CALLOUT),
    ('BOX', (0,0), (-1,-1), 1.2, BORDER_CALLOUT),
    ('PADDING', (0,0), (-1,-1), 8),
]))
story.append(t_gps_policy)

story.append(Spacer(1, 6))

# ================= SECTION 3: SHIFTS, OVERTIME & LEAVES =================
story.append(Paragraph("3. Shifts, Overtime & Leave Management", style_h1))
story.append(HRFlowable(width="100%", thickness=1.5, color=BLUE, spaceAfter=8))

story.append(Paragraph("3.1 Shift Templates & Roster Assignments", style_h2))
story.append(Paragraph("<b>• Shift Templates:</b> Owners create shift definitions specifying timing windows (e.g. 09:00 AM - 05:00 PM), assigned weekdays (Everyday, Mon-Fri, Weekdays, Weekends), and half-day thresholds.", style_bullet))
story.append(Paragraph("<b>• Pay Rates:</b> Assigning an employee to a shift configures custom pay rates for Present days, Half-days, Approved Leaves, and Absent penalties.", style_bullet))
story.append(Paragraph("<b>• Auto Weekly Offs:</b> Days of the week not assigned to a shift automatically become the employee's Weekly Off days.", style_bullet))

story.append(Paragraph("3.2 Pre-Approved Overtime", style_h2))
story.append(Paragraph("Owners raise pre-approved overtime sessions specifying the staff member, work date, start/end time, and agreed overtime payment amount (in ₹). Employees can then check in for the overtime session.", style_bullet))

story.append(Paragraph("3.3 Leave Request Workflow", style_h2))
story.append(Paragraph("Employees view their 14-day upcoming schedule and apply for leave on specific shift dates. Owners review requests on their dashboard and approve or reject them with remarks. Approving a leave updates attendance and accrues leave pay automatically.", style_bullet))

story.append(Spacer(1, 6))

# ================= SECTION 4: SALARY LEDGER & PAYROLL =================
story.append(Paragraph("4. Earning Ledger & Monthly Payroll Engine", style_h1))
story.append(HRFlowable(width="100%", thickness=1.5, color=BLUE, spaceAfter=8))

story.append(Paragraph(
    "Presence maintains an itemized append-only earning ledger for every employee. All monetary calculations are performed in exact whole integer units (INR ₹) with zero decimal floating-point rounding errors.",
    style_body
))

ledger_types_data = [
    [Paragraph("<b>Earning Type</b>", style_table_head), Paragraph("<b>Trigger Event</b>", style_table_head), Paragraph("<b>Payment Rule</b>", style_table_head)],
    [Paragraph("<b>SHIFT</b>", style_table_cell_bold), Paragraph("Shift Clock-Out or Approved Paid Leave", style_table_cell), Paragraph("Accrues shift present_pay, half_day_pay, or leave_pay.", style_table_cell)],
    [Paragraph("<b>HOLIDAY</b>", style_table_cell_bold), Paragraph("Unworked Paid Holiday", style_table_cell), Paragraph("Accrues holiday payment rule amount (or daily rate).", style_table_cell)],
    [Paragraph("<b>OFF_DAY</b>", style_table_cell_bold), Paragraph("Unworked Weekly Off Day", style_table_cell), Paragraph("Accrues weekly off payment rule amount (or daily rate).", style_table_cell)],
    [Paragraph("<b>OVERTIME</b>", style_table_cell_bold), Paragraph("Completed Overtime Session", style_table_cell), Paragraph("Accrues approved overtime payment amount.", style_table_cell)]
]
t_ledger = Table(ledger_types_data, colWidths=[90, 205, 220])
t_ledger.setStyle(TableStyle([
    ('BACKGROUND', (0,0), (-1,0), colors.HexColor('#D97706')),
    ('BOX', (0,0), (-1,-1), 1, colors.HexColor('#FDE68A')),
    ('INNERGRID', (0,0), (-1,-1), 0.5, colors.HexColor('#FEF3C7')),
    ('PADDING', (0,0), (-1,-1), 5),
    ('ROWBACKGROUNDS', (0,1), (-1,-1), [colors.white, colors.HexColor('#FFFDF5')])
]))
story.append(t_ledger)

story.append(Paragraph("4.1 Monthly Payroll Reconciliation & Payslips", style_h2))
story.append(Paragraph("<b>• Automated Reconciliation:</b> Monthly payroll totals automatically calculate Total Earned Balance minus Total Payments Disbursed to show remaining Due Balance.", style_bullet))
story.append(Paragraph("<b>• Recording Payments:</b> Owners disburse full or partial payments specifying payment date, method (Cash, Bank Transfer, UPI), and reference number.", style_bullet))
story.append(Paragraph("<b>• PDF Payslips:</b> Staff and Owners can view itemized monthly ledger breakdowns and download official PDF payslips.", style_bullet))

story.append(Spacer(1, 6))

# ================= SECTION 5: REAL-WORLD OPERATIONAL SCENARIOS =================
story.append(Paragraph("5. Real-World Scenarios & System Behaviors", style_h1))
story.append(HRFlowable(width="100%", thickness=1.5, color=BLUE, spaceAfter=8))

scenarios_data = [
    [Paragraph("<b>Operational Scenario</b>", style_table_head), Paragraph("<b>How Presence Handles It</b>", style_table_head)],
    [
        Paragraph("<b>1. Employee Clocking In at Workplace</b>", style_table_cell_bold),
        Paragraph("Employee opens app at workplace and taps Clock In. App checks GPS coordinates against workplace geofence (within 50m) and matches camera face scan against enrolled face template. When verified, clock-in is recorded as PRESENT.", style_table_cell)
    ],
    [
        Paragraph("<b>2. Working an Overnight Shift</b>", style_table_cell_bold),
        Paragraph("For overnight shifts (e.g. 10:00 PM to 06:00 AM), an employee clocking in after midnight (e.g. 01:00 AM) is automatically attributed back to yesterday's shift start date. Earnings and records map accurately to the shift work date.", style_table_cell)
    ],
    [
        Paragraph("<b>3. Going Outside Geofence During Shift</b>", style_table_cell_bold),
        Paragraph("Foreground service checks location every ~10 mins. If 3 consecutive checks are outside, an alert appears on the Owner's Live Monitor. The Owner can override attendance (ABSENT, HALF_DAY, LEAVE). If no action is taken before shift ends, status defaults to PRESENT.", style_table_cell)
    ],
    [
        Paragraph("<b>4. Working on a Holiday or Off-Day</b>", style_table_cell_bold),
        Paragraph("An unworked paid holiday or weekly off accrues base rule pay. If the employee comes in and works a shift on a holiday or off-day, it is recorded as a worked shift and earns full shift pay.", style_table_cell)
    ],
    [
        Paragraph("<b>5. Applying for Leave</b>", style_table_cell_bold),
        Paragraph("Employee selects shift dates from their 14-day schedule and submits a leave request. Owner receives an in-app notification to Approve or Reject. Approval automatically sets attendance to LEAVE and credits leave pay.", style_table_cell)
    ],
    [
        Paragraph("<b>6. Booking & Working Overtime</b>", style_table_cell_bold),
        Paragraph("Owner creates a pre-approved overtime session for a staff member. On the scheduled date, employee taps Clock In for Overtime. Upon clock-out, overtime duration is completed and overtime pay is credited to their ledger.", style_table_cell)
    ],
    [
        Paragraph("<b>7. Monthly Salary Disbursement</b>", style_table_cell_bold),
        Paragraph("Owner opens Payroll screen, views total monthly earnings and due balance for each staff member, taps Pay Staff, records payment details (UPI/Bank/Cash), and system issues a downloadable PDF payslip.", style_table_cell)
    ],
    [
        Paragraph("<b>8. Signing In on a New Phone</b>", style_table_cell_bold),
        Paragraph("Presence permits exactly 1 active device session per user. Signing in on a new phone automatically invalidates the session on the old phone, ensuring session security.", style_table_cell)
    ]
]

t_scenarios = Table(scenarios_data, colWidths=[155, 360])
t_scenarios.setStyle(TableStyle([
    ('BACKGROUND', (0,0), (-1,0), NAVY),
    ('BOX', (0,0), (-1,-1), 1, colors.HexColor('#CBD5E1')),
    ('INNERGRID', (0,0), (-1,-1), 0.5, colors.HexColor('#E2E8F0')),
    ('PADDING', (0,0), (-1,-1), 6),
    ('VALIGN', (0,0), (-1,-1), 'TOP'),
    ('ROWBACKGROUNDS', (0,1), (-1,-1), [colors.white, BG_LIGHT])
]))
story.append(t_scenarios)

story.append(Spacer(1, 10))

# ================= SECTION 6: SCREEN-BY-SCREEN USER GUIDE =================
story.append(Paragraph("6. Screen-by-Screen User Interface Guide", style_h1))
story.append(HRFlowable(width="100%", thickness=1.5, color=BLUE, spaceAfter=8))

story.append(Paragraph("6.1 Owner Dashboard Tabs & Features", style_h2))
story.append(Paragraph("<b>• Live Monitor Tab (LiveMonitorTab.kt):</b> Displays real-time presence cards for all staff on duty today, Inside/Outside geofence indicators, recent location check pings, exception badges, and manual resolution buttons (ABSENT, HALF_DAY, LEAVE, RESUME).", style_bullet))
story.append(Paragraph("<b>• Workforce Roster (EmployeesManagementScreen.kt):</b> Roster of all employees across workplace premises. Shows cumulative earned ledger balance, paid disbursements, and due amount. Features 'Assign Shift' button launching AssignShiftWithRulesModal.kt.", style_bullet))
story.append(Paragraph("<b>• Shift Manager (ShiftManagementScreen.kt):</b> Create and edit shift templates, timing windows, weekday assignment patterns, and half-day thresholds.", style_bullet))
story.append(Paragraph("<b>• Approvals & Overtime (OwnerLeaveScreen.kt, OwnerOvertimeScreen.kt):</b> Review employee leave requests with approve/reject remarks. Book pre-approved overtime sessions specifying staff member, work date, time, and payment.", style_bullet))
story.append(Paragraph("<b>• Payroll & Disbursements (PayrollScreen.kt, PayStaffModal.kt):</b> Reconcile monthly staff payrolls, record payments (Cash/Bank/UPI), view daily itemized earning ledgers, and export PDF/CSV reports.", style_bullet))

story.append(Spacer(1, 6))

story.append(Paragraph("6.2 Employee Dashboard Tabs & Features", style_h2))
story.append(Paragraph("<b>• Clock-In Scanner (EmployeeClockInTab.kt):</b> Live camera scanner performing face matching and GPS geofence validation for active shift or overtime check-in.", style_bullet))
story.append(Paragraph("<b>• My Attendance (MyAttendanceScreen.kt):</b> Calendar and list view of daily attendance records, shift start/end timestamps, worked duration, and late minutes.", style_bullet))
story.append(Paragraph("<b>• My Schedule & Leave (MyShiftsScreen.kt, EmployeeLeaveScreen.kt):</b> View 14-day schedule projection (assigned shifts, holidays, weekly offs) and submit leave applications.", style_bullet))
story.append(Paragraph("<b>• Earning Ledger & Payslips (EmployeeLedgerTab.kt):</b> Track daily itemized earned breakdown (SHIFT, HOLIDAY, OFF_DAY, OVERTIME) and download official monthly payslip PDF statements.", style_bullet))

# BUILD DOCUMENT WITH NUMBERED CANVAS
doc.build(story, canvasmaker=NumberedCanvas)
print(f"ReportLab User Guide PDF successfully built: {pdf_output_name}")
