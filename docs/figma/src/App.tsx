import { useEffect, useMemo, useState, type FormEvent, type ReactNode } from "react";
import {
  Activity,
  Bell,
  CalendarDays,
  Camera,
  Check,
  ChevronDown,
  ChevronLeft,
  ChevronRight,
  CircleDollarSign,
  Clock3,
  Computer,
  Download,
  Eye,
  EyeOff,
  FileBarChart,
  FileText,
  Gauge,
  HardDrive,
  KeyRound,
  Laptop,
  LogOut,
  Menu,
  MonitorCog,
  MoreHorizontal,
  PackageCheck,
  Pencil,
  Plus,
  Search,
  Settings,
  ShieldCheck,
  SlidersHorizontal,
  Trash2,
  Upload,
  User,
  UserCog,
  Users,
  Wrench,
  X,
} from "lucide-react";

type Page =
  | "Dashboard"
  | "Customers"
  | "Computers"
  | "Appointments"
  | "Repair Requests"
  | "Services"
  | "Technicians"
  | "Payments"
  | "Reports"
  | "User Accounts"
  | "Settings"
  | "My Profile"
  | "Account Settings"
  | "Change Password"
  | "Login Activity";

type IconType = typeof Gauge;
type Row = Record<string, string | number>;
type Role = "Admin" | "Customer" | "Technician";
type CustomerPage =
  | "Home"
  | "My Computers"
  | "Appointments"
  | "My Repairs"
  | "Services & Pricing"
  | "Payments & Receipts"
  | "Profile";
type TechnicianPage = "My Assigned Repairs" | "My Schedule" | "Profile";
type RepairDecision = "pending" | "approved" | "declined";

type DemoData = {
  customers: Row[];
  computers: Row[];
  appointments: Row[];
  repairs: Row[];
  services: Row[];
  technicians: Row[];
  payments: Row[];
  users: Row[];
  repair: {
    id: string;
    stage: number;
    customerId: string;
    deviceId: string;
    appointmentId: string;
    issue: string;
    photos: string[];
    condition: string;
    accessories: string;
    technician: string;
    tests: string;
    diagnosis: string;
    estimateParts: number;
    estimateLabor: number;
    decision: RepairDecision;
    workNotes: string;
    paid: boolean;
    receipt: string;
    collected: boolean;
  };
  customerProfile: {
    name: string;
    email: string;
    phone: string;
    address: string;
  };
  updatedAt: string;
};

const repairStages = [
  "Appointment requested",
  "Appointment confirmed",
  "Checked in & assigned",
  "Estimate awaiting approval",
  "Repair approved",
  "Technical work complete",
  "Ready for pickup",
  "Collected",
] as const;

const navGroups: { label: string; items: { label: Page; icon: IconType }[] }[] = [
  { label: "", items: [{ label: "Dashboard", icon: Gauge }] },
  {
    label: "MANAGEMENT",
    items: [
      { label: "Customers", icon: Users },
      { label: "Computers", icon: Computer },
      { label: "Appointments", icon: CalendarDays },
      { label: "Repair Requests", icon: FileText },
      { label: "Technicians", icon: UserCog },
      { label: "Services", icon: Wrench },
      { label: "Payments", icon: CircleDollarSign },
    ],
  },
  { label: "REPORTS", items: [{ label: "Reports", icon: FileBarChart }] },
  {
    label: "SYSTEM",
    items: [
      { label: "User Accounts", icon: Users },
      { label: "Settings", icon: Settings },
    ],
  },
];

const people = [
  {
    id: "C-001",
    name: "Ara Bernabe",
    phone: "0927 123 4567",
    email: "ara.bernabe@email.com",
    address: "Laoag, Ilocos Norte",
    devices: 2,
    repairs: 5,
    status: "Active",
    added: "Jan 15, 2026",
  },
  {
    id: "C-002",
    name: "Roniel Bisac",
    phone: "0918 234 5678",
    email: "roniel.bisac@email.com",
    address: "Bacarra, Ilocos Norte",
    devices: 3,
    repairs: 8,
    status: "Active",
    added: "Feb 03, 2026",
  },
  {
    id: "C-003",
    name: "Yra Gamno",
    phone: "0905 345 6789",
    email: "yra.gamno@email.com",
    address: "Paoay, Ilocos Norte",
    devices: 1,
    repairs: 3,
    status: "Active",
    added: "Feb 18, 2026",
  },
  {
    id: "C-004",
    name: "Clifford Luis",
    phone: "0936 456 7890",
    email: "clifford.luis@email.com",
    address: "Sarrat, Ilocos Norte",
    devices: 2,
    repairs: 6,
    status: "Active",
    added: "Mar 02, 2026",
  },
  {
    id: "C-005",
    name: "AC Halabaso",
    phone: "0917 567 8901",
    email: "ac.halabaso@email.com",
    address: "Marcos, Ilocos Norte",
    devices: 1,
    repairs: 4,
    status: "Active",
    added: "Mar 15, 2026",
  },
  {
    id: "C-006",
    name: "Jansen Yoro",
    phone: "0998 678 9012",
    email: "jansen.yoro@email.com",
    address: "Banna, Ilocos Norte",
    devices: 3,
    repairs: 7,
    status: "Active",
    added: "Apr 21, 2026",
  },
  {
    id: "C-007",
    name: "Peng Gormoco",
    phone: "0916 789 0123",
    email: "peng.gormoco@email.com",
    address: "Pinili, Ilocos Norte",
    devices: 2,
    repairs: 5,
    status: "Inactive",
    added: "Apr 12, 2026",
  },
  {
    id: "C-008",
    name: "Gina Cole",
    phone: "0920 890 1234",
    email: "gina.cole@email.com",
    address: "San Nicolas, Ilocos Norte",
    devices: 1,
    repairs: 2,
    status: "Inactive",
    added: "Apr 20, 2026",
  },
];

const computers = [
  {
    id: "D-001",
    customer: "Ara Bernabe",
    type: "Laptop",
    brand: "ACER",
    model: "Predator Helios Neo 16",
    serial: "ACERPH00123",
    status: "Active",
    added: "Jan 15, 2026",
  },
  {
    id: "D-002",
    customer: "Roniel Bisac",
    type: "PC",
    brand: "Apple",
    model: "Mac Mini M2",
    serial: "MACMIN04567",
    status: "Active",
    added: "Feb 03, 2026",
  },
  {
    id: "D-003",
    customer: "Yra Gamno",
    type: "PC",
    brand: "HP",
    model: "Pavilion Desktop",
    serial: "HPDTOP8901",
    status: "In Repair",
    added: "Feb 18, 2026",
  },
  {
    id: "D-004",
    customer: "Clifford Luis",
    type: "PC",
    brand: "LENOVO",
    model: "IdeaCentre 5",
    serial: "LNV0098765",
    status: "Waiting for Parts",
    added: "Mar 02, 2026",
  },
  {
    id: "D-005",
    customer: "AC Halabaso",
    type: "Laptop",
    brand: "ASUS",
    model: "TUF Gaming A15",
    serial: "ASUSTUF1122",
    status: "Under Diagnosis",
    added: "Mar 15, 2026",
  },
  {
    id: "D-006",
    customer: "Jansen Yoro",
    type: "Laptop",
    brand: "MSI",
    model: "Katana 15",
    serial: "MSIKTN3345",
    status: "Ready for Pickup",
    added: "Apr 01, 2026",
  },
  {
    id: "D-007",
    customer: "Peng Gormoco",
    type: "PC",
    brand: "Dell",
    model: "OptiPlex 7090",
    serial: "DELL7090332",
    status: "Inactive",
    added: "Apr 12, 2026",
  },
  {
    id: "D-008",
    customer: "Gina Cole",
    type: "Laptop",
    brand: "HP",
    model: "Victus 15",
    serial: "HPVCT5566",
    status: "Inactive",
    added: "Apr 20, 2026",
  },
];

const appointments = [
  {
    id: "A-001",
    date: "Oct 16, 2026 · 10:00 AM",
    customer: "Ara Bernabe",
    device: "Laptop (ACER)",
    service: "Hardware Repair",
    technician: "Vina Saji Fukiko",
    status: "Confirmed",
    notes: "No notes",
  },
  {
    id: "A-002",
    date: "Oct 16, 2026 · 11:30 AM",
    customer: "Roniel Bisac",
    device: "PC (Mac Mini)",
    service: "OS Reinstallation",
    technician: "Ma. Anne Tote Fukiko",
    status: "Confirmed",
    notes: "Bring charger",
  },
  {
    id: "A-003",
    date: "Oct 16, 2026 · 1:00 PM",
    customer: "Yra Gamno",
    device: "PC (HP)",
    service: "Virus & Malware Removal",
    technician: "Mee Khotou Fukiko",
    status: "Pending",
    notes: "For check-up",
  },
  {
    id: "A-004",
    date: "Oct 16, 2026 · 2:30 PM",
    customer: "Clifford Luis",
    device: "PC (LENOVO)",
    service: "Hardware Repair",
    technician: "Keena Mhot Fukiko",
    status: "Confirmed",
    notes: "Replace HDD",
  },
  {
    id: "A-005",
    date: "Oct 16, 2026 · 4:00 PM",
    customer: "AC Halabaso",
    device: "Laptop (ASUS)",
    service: "System Upgrade",
    technician: "Mee Zhu Ghiat Fukiko",
    status: "Pending",
    notes: "Install RAM",
  },
  {
    id: "A-006",
    date: "Oct 17, 2026 · 10:00 AM",
    customer: "Jansen Yoro",
    device: "Laptop (MSI)",
    service: "General Check-up",
    technician: "Nhu Mah Gah Fukiko",
    status: "Confirmed",
    notes: "No notes",
  },
];

const technicians = [
  {
    id: "N-001",
    name: "Ma. Anne Tote Fukiko",
    phone: "0917 123 4567",
    email: "anne.fukiko@email.com",
    specialization: "OS Installation, Software Repair",
    status: "Active",
    assignments: 3,
  },
  {
    id: "N-002",
    name: "Vina Saji Fukiko",
    phone: "0918 234 5678",
    email: "vina.fukiko@email.com",
    specialization: "Hardware Repair, Diagnostics",
    status: "Active",
    assignments: 2,
  },
  {
    id: "N-003",
    name: "Mee Lhangham Fukiko",
    phone: "0905 345 6789",
    email: "lhangham.fukiko@email.com",
    specialization: "Virus Removal, OS Reinstallation",
    status: "Active",
    assignments: 3,
  },
  {
    id: "N-004",
    name: "Ma. Vha Hoe Fukiko",
    phone: "0936 456 7890",
    email: "vha.fukiko@email.com",
    specialization: "Hardware Repair, Part Replacement",
    status: "Active",
    assignments: 2,
  },
  {
    id: "N-005",
    name: "Mee Khotou Fukiko",
    phone: "0917 567 8901",
    email: "khotou.fukiko@email.com",
    specialization: "System Upgrade, Diagnostics",
    status: "Active",
    assignments: 2,
  },
  {
    id: "N-006",
    name: "Keena Mhot Fukiko",
    phone: "0998 678 9012",
    email: "keena.fukiko@email.com",
    specialization: "Laptop Repair, Hardware Repair",
    status: "Active",
    assignments: 3,
  },
];

const services = [
  {
    id: "S-001",
    service: "Hardware Diagnostics",
    category: "Diagnostics",
    duration: "1 hour",
    price: "₱500.00",
    status: "Active",
  },
  {
    id: "S-002",
    service: "Hardware Repair",
    category: "Repair",
    duration: "2–4 hours",
    price: "₱1,200.00",
    status: "Active",
  },
  {
    id: "S-003",
    service: "OS Reinstallation",
    category: "Software",
    duration: "2 hours",
    price: "₱800.00",
    status: "Active",
  },
  {
    id: "S-004",
    service: "Virus & Malware Removal",
    category: "Software",
    duration: "2 hours",
    price: "₱700.00",
    status: "Active",
  },
  {
    id: "S-005",
    service: "System Upgrade",
    category: "Upgrade",
    duration: "3 hours",
    price: "₱1,000.00",
    status: "Active",
  },
  {
    id: "S-006",
    service: "General Check-up",
    category: "Maintenance",
    duration: "1 hour",
    price: "₱500.00",
    status: "Active",
  },
];

const repairs = [
  {
    id: "R-1024",
    customer: "Ara Bernabe",
    device: "ACER Predator Helios",
    issue: "No display after startup",
    technician: "Vina Saji Fukiko",
    cost: "₱4,850.00",
    status: "In Progress",
    updated: "Today, 9:42 AM",
  },
  {
    id: "R-1023",
    customer: "Roniel Bisac",
    device: "Apple Mac Mini M2",
    issue: "macOS boot failure",
    technician: "Ma. Anne Tote Fukiko",
    cost: "₱1,800.00",
    status: "Diagnosing",
    updated: "Today, 8:15 AM",
  },
  {
    id: "R-1022",
    customer: "Yra Gamno",
    device: "HP Pavilion Desktop",
    issue: "Malware and pop-ups",
    technician: "Mee Khotou Fukiko",
    cost: "₱700.00",
    status: "Ready for Pickup",
    updated: "Yesterday",
  },
  {
    id: "R-1021",
    customer: "Clifford Luis",
    device: "Lenovo IdeaCentre 5",
    issue: "Faulty hard drive",
    technician: "Keena Mhot Fukiko",
    cost: "₱3,200.00",
    status: "Waiting for Parts",
    updated: "Oct 14, 2026",
  },
  {
    id: "R-1020",
    customer: "AC Halabaso",
    device: "ASUS TUF Gaming A15",
    issue: "Memory upgrade",
    technician: "Mee Zhu Ghiat Fukiko",
    cost: "₱2,900.00",
    status: "Completed",
    updated: "Oct 13, 2026",
  },
];

const payments = [
  {
    id: "P-2026-184",
    date: "Oct 16, 2026",
    customer: "Ara Bernabe",
    repair: "R-1024",
    method: "GCash",
    amount: "₱4,850.00",
    status: "Paid",
    receipt: "OR-0184",
  },
  {
    id: "P-2026-183",
    date: "Oct 16, 2026",
    customer: "Roniel Bisac",
    repair: "R-1023",
    method: "Cash",
    amount: "₱1,800.00",
    status: "Paid",
    receipt: "OR-0183",
  },
  {
    id: "P-2026-182",
    date: "Oct 15, 2026",
    customer: "Yra Gamno",
    repair: "R-1022",
    method: "Maya",
    amount: "₱700.00",
    status: "Paid",
    receipt: "OR-0182",
  },
  {
    id: "P-2026-181",
    date: "Oct 15, 2026",
    customer: "Clifford Luis",
    repair: "R-1021",
    method: "Cash",
    amount: "₱1,600.00",
    status: "Partial",
    receipt: "OR-0181",
  },
  {
    id: "P-2026-180",
    date: "Oct 14, 2026",
    customer: "AC Halabaso",
    repair: "R-1020",
    method: "Card",
    amount: "₱2,900.00",
    status: "Paid",
    receipt: "OR-0180",
  },
];

const users = [
  {
    id: "U-001",
    name: "Pablo Job",
    username: "pablo.job",
    email: "pablo.job@crsm.com",
    role: "Administrator",
    status: "Active",
    created: "Oct 01, 2026",
    login: "Oct 16, 2026 09:45 AM",
  },
  ...technicians.slice(0, 5).map((t, i) => ({
    id: `U-00${i + 2}`,
    name: t.name,
    username: t.name.split(" ")[0].toLowerCase(),
    email: t.email,
    role: "Technician",
    status: "Active",
    created: "Oct 01, 2026",
    login: `Oct ${15 - i}, 2026 0${8 + i}:30 AM`,
  })),
  {
    id: "U-007",
    name: "Jack Cole",
    username: "jack.cole",
    email: "jack.cole@crsm.com",
    role: "Staff",
    status: "Active",
    created: "Oct 01, 2026",
    login: "Oct 14, 2026 08:50 AM",
  },
];

const DEMO_STORAGE_KEY = "crsm-demo-data-v3";

function createDemoData(): DemoData {
  return {
    customers: people.map((item) => ({ ...item })),
    computers: computers.map((item) => ({ ...item })),
    appointments: appointments.map((item) =>
      item.id === "A-001" ? { ...item, status: "Pending" } : { ...item },
    ),
    repairs: repairs.map((item) =>
      item.id === "R-1024"
        ? { ...item, technician: "", status: "Appointment Requested", cost: "₱0.00" }
        : { ...item },
    ),
    services: services.map((item) => ({
      ...item,
      description: `Professional ${item.service.toLowerCase()} with testing and a clear service report.`,
    })),
    technicians: technicians.map((item) => ({ ...item })),
    payments: payments.map((item) => ({ ...item })),
    users: [
      ...users.map((item) => ({ ...item })),
      {
        id: "U-100",
        name: "Ara Bernabe",
        username: "ara.bernabe",
        email: "ara.bernabe@email.com",
        role: "Customer",
        status: "Active",
        created: "Oct 16, 2026",
        login: "Demo account",
      },
    ],
    repair: {
      id: "R-1024",
      stage: 0,
      customerId: "C-001",
      deviceId: "D-001",
      appointmentId: "A-001",
      issue: "Laptop powers on, but the screen remains black after startup.",
      photos: [],
      condition: "",
      accessories: "",
      technician: "",
      tests: "",
      diagnosis: "",
      estimateParts: 3850,
      estimateLabor: 1000,
      decision: "pending",
      workNotes: "",
      paid: false,
      receipt: "",
      collected: false,
    },
    customerProfile: {
      name: "Ara Bernabe",
      email: "ara.bernabe@email.com",
      phone: "0927 123 4567",
      address: "Laoag, Ilocos Norte",
    },
    updatedAt: new Date().toISOString(),
  };
}

function useDemoData() {
  const [data, setData] = useState<DemoData>(() => {
    try {
      const saved = localStorage.getItem(DEMO_STORAGE_KEY);
      return saved ? (JSON.parse(saved) as DemoData) : createDemoData();
    } catch {
      return createDemoData();
    }
  });
  useEffect(() => {
    try {
      localStorage.setItem(DEMO_STORAGE_KEY, JSON.stringify(data));
    } catch {
      // Keep the in-memory session usable if a browser's storage quota is exhausted by photos.
    }
  }, [data]);
  const update = (updater: (current: DemoData) => DemoData) => {
    setData((current) => ({ ...updater(current), updatedAt: new Date().toISOString() }));
  };
  const reset = () => {
    const fresh = createDemoData();
    localStorage.setItem(DEMO_STORAGE_KEY, JSON.stringify(fresh));
    setData(fresh);
  };
  return { data, update, reset };
}

const pageMeta: Record<Page, { subtitle: string; icon: IconType }> = {
  Dashboard: { subtitle: "A quick look at your repair center today.", icon: Gauge },
  Customers: { subtitle: "Manage customer information and computer records.", icon: Users },
  Computers: {
    subtitle: "Manage computer and device records with their assigned customers.",
    icon: Computer,
  },
  Appointments: {
    subtitle: "Schedule and manage customer appointments for computer repair services.",
    icon: CalendarDays,
  },
  "Repair Requests": {
    subtitle: "Track diagnostics, repair progress, parts, and completion.",
    icon: FileText,
  },
  Services: {
    subtitle: "Manage repair services, categories, duration, and pricing.",
    icon: Wrench,
  },
  Technicians: {
    subtitle: "Manage technician accounts, skills, availability, and assigned tasks.",
    icon: UserCog,
  },
  Payments: {
    subtitle: "Track customer payments, balances, methods, and receipts.",
    icon: CircleDollarSign,
  },
  Reports: { subtitle: "Review revenue, operations, and service performance.", icon: FileBarChart },
  "User Accounts": {
    subtitle: "Manage system user accounts, roles, and access permissions.",
    icon: Users,
  },
  Settings: { subtitle: "Manage system settings, preferences, and configuration.", icon: Settings },
  "My Profile": { subtitle: "View and update your personal information.", icon: User },
  "Account Settings": {
    subtitle: "Manage your account preferences and system settings.",
    icon: SlidersHorizontal,
  },
  "Change Password": {
    subtitle: "Update your password to keep your account secure.",
    icon: KeyRound,
  },
  "Login Activity": { subtitle: "Review recent sign-ins and active sessions.", icon: Activity },
};

function Logo({ compact = false }: { compact?: boolean }) {
  return (
    <div className={`brand ${compact ? "brand--compact" : ""}`}>
      <div className="brand-mark">
        <MonitorCog size={compact ? 24 : 31} strokeWidth={1.8} />
      </div>
      <div>
        <strong>CRSM</strong>
        {!compact && (
          <span>
            COMPUTER REPAIR
            <br />
            SERVICE MANAGEMENT
          </span>
        )}
      </div>
    </div>
  );
}

function Status({ value }: { value: string }) {
  const kind = ["Active", "Paid", "Confirmed", "Completed", "Ready for Pickup"].includes(value)
    ? "success"
    : ["Pending", "Partial", "Waiting for Parts"].includes(value)
      ? "warning"
      : ["Inactive", "Cancelled", "Overdue"].includes(value)
        ? "danger"
        : "info";
  return <span className={`status status--${kind}`}>{value}</span>;
}

function Avatar({ name, size = "sm" }: { name: string; size?: "sm" | "lg" }) {
  return (
    <span className={`avatar avatar--${size}`}>
      {name
        .split(" ")
        .slice(0, 2)
        .map((v) => v[0])
        .join("")}
    </span>
  );
}

function Sidebar({
  page,
  onPage,
  open,
  onClose,
  onLogout,
}: {
  page: Page;
  onPage: (p: Page) => void;
  open: boolean;
  onClose: () => void;
  onLogout: () => void;
}) {
  return (
    <aside className={`sidebar ${open ? "sidebar--open" : ""}`}>
      <button className="mobile-close" onClick={onClose} aria-label="Close menu">
        <X size={20} />
      </button>
      <Logo />
      <nav>
        {navGroups.map((group) => (
          <div className="nav-group" key={group.label || "main"}>
            {group.label && <div className="nav-label">{group.label}</div>}
            {group.items.map(({ label, icon: Icon }) => (
              <button
                key={label}
                onClick={() => {
                  onPage(label);
                  onClose();
                }}
                className={page === label ? "active" : ""}
              >
                <Icon size={17} />
                <span>{label}</span>
              </button>
            ))}
          </div>
        ))}
      </nav>
      <div className="sidebar-watermark">
        <MonitorCog size={62} />
        <strong>CRSM</strong>
      </div>
      <button className="logout" onClick={onLogout}>
        <LogOut size={17} /> Logout
      </button>
    </aside>
  );
}

function Topbar({
  onMenu,
  onPage,
  onSignOut,
}: {
  onMenu: () => void;
  onPage: (p: Page) => void;
  onSignOut: () => void;
}) {
  const [menu, setMenu] = useState(false);
  const [search, setSearch] = useState("");
  const runSearch = () => {
    const value = search.toLowerCase();
    if (value.includes("customer")) onPage("Customers");
    else if (value.includes("payment") || value.includes("receipt")) onPage("Payments");
    else if (value.includes("computer") || value.includes("device")) onPage("Computers");
    else if (value.includes("repair")) onPage("Repair Requests");
    else onPage("Appointments");
  };
  return (
    <header className="topbar">
      <button className="menu-button" onClick={onMenu} aria-label="Open menu">
        <Menu size={20} />
      </button>
      <label className="global-search">
        <Search size={15} />
        <input
          aria-label="Global search"
          placeholder="Search customers, repairs, appointments..."
          value={search}
          onChange={(event) => setSearch(event.target.value)}
          onKeyDown={(event) => {
            if (event.key === "Enter") runSearch();
          }}
        />
      </label>
      <div className="topbar-actions">
        <button
          className="icon-button notification"
          aria-label="Notifications"
          onClick={() => onPage("Appointments")}
        >
          <Bell size={18} />
          <span />
        </button>
        <button className="profile-trigger" onClick={() => setMenu(!menu)}>
          <Avatar name="Pablo Job" />
          <span>
            <b>Pablo Job</b>
            <small>Administrator</small>
          </span>
          <ChevronDown size={15} />
        </button>
        {menu && (
          <div className="profile-menu">
            <div className="profile-menu-head">
              <Avatar name="Pablo Job" />
              <span>
                <b>Pablo Job</b>
                <small>Administrator</small>
              </span>
            </div>
            {(["My Profile", "Account Settings", "Change Password"] as Page[]).map((p) => (
              <button
                key={p}
                onClick={() => {
                  onPage(p);
                  setMenu(false);
                }}
              >
                <User size={15} />
                {p}
              </button>
            ))}
            <button onClick={onSignOut} className="danger-text">
              <LogOut size={15} />
              Sign out
            </button>
          </div>
        )}
      </div>
    </header>
  );
}

function PageHeader({ page, action }: { page: Page; action?: ReactNode }) {
  const meta = pageMeta[page];
  const Icon = meta.icon;
  return (
    <section className="page-header">
      <div className="page-header-copy">
        <span className="header-icon">
          <Icon size={24} />
        </span>
        <div>
          <h1>{page}</h1>
          <p>{meta.subtitle}</p>
        </div>
      </div>
      <div className="header-right">
        {action}
        <div className="date-block">
          <CalendarDays size={17} />
          <span>
            <small>Thu, Oct 16, 2026</small>
            <b>10:24 AM</b>
          </span>
        </div>
      </div>
    </section>
  );
}

function SummaryCard({
  icon: Icon,
  label,
  value,
  trend,
  tone = "blue",
}: {
  icon: IconType;
  label: string;
  value: string;
  trend: string;
  tone?: string;
}) {
  return (
    <article className={`summary-card summary-card--${tone}`}>
      <span className="summary-icon">
        <Icon size={23} />
      </span>
      <div>
        <small>{label}</small>
        <strong>{value}</strong>
        <em>{trend}</em>
      </div>
    </article>
  );
}

function TableView({
  rows,
  columns,
  name,
  onSave,
  onDelete,
}: {
  rows: Row[];
  columns: [string, string][];
  name: string;
  onSave: (row: Row) => void;
  onDelete: (row: Row) => void;
}) {
  const [query, setQuery] = useState("");
  const [status, setStatus] = useState("All status");
  const [sort, setSort] = useState("newest");
  const [pageNumber, setPageNumber] = useState(1);
  const [editing, setEditing] = useState<Row | null>(null);
  const [viewing, setViewing] = useState<Row | null>(null);
  const [confirming, setConfirming] = useState<Row | null>(null);
  const [error, setError] = useState("");
  const pageSize = 5;
  const filtered = rows
    .filter((row) => Object.values(row).join(" ").toLowerCase().includes(query.toLowerCase()))
    .filter((row) => status === "All status" || String(row.status) === status)
    .sort((a, b) => {
      const left = String(a.name || a.customer || a.service || a.id || "");
      const right = String(b.name || b.customer || b.service || b.id || "");
      return sort === "name" ? left.localeCompare(right) : right.localeCompare(left);
    });
  const pageCount = Math.max(1, Math.ceil(filtered.length / pageSize));
  const safePage = Math.min(pageNumber, pageCount);
  const visible = filtered.slice((safePage - 1) * pageSize, safePage * pageSize);
  const statuses = Array.from(new Set(rows.map((row) => String(row.status || "")).filter(Boolean)));
  const startEdit = (row?: Row) => {
    setError("");
    setEditing(
      row
        ? { ...row }
        : Object.fromEntries(
            columns.map(([key], index) => [
              key,
              key === "id"
                ? `${name.slice(0, 1).toUpperCase()}-${String(rows.length + 1).padStart(3, "0")}`
                : index === 1
                  ? ""
                  : key === "status"
                    ? "Active"
                    : "",
            ]),
          ),
    );
  };
  const submit = () => {
    if (!editing) return;
    const required = columns.slice(0, 2).map(([key]) => String(editing[key] || "").trim());
    if (required.some((value) => !value)) {
      setError("Complete the required ID and name fields before saving.");
      return;
    }
    onSave(editing);
    setEditing(null);
  };
  return (
    <section className="table-card">
      <div className="table-tools">
        <label className="search-field">
          <Search size={15} />
          <input
            value={query}
            onChange={(e) => {
              setQuery(e.target.value);
              setPageNumber(1);
            }}
            placeholder={`Search ${name.toLowerCase()} by ID, name, or details...`}
          />
        </label>
        <select
          aria-label="Status filter"
          value={status}
          onChange={(event) => {
            setStatus(event.target.value);
            setPageNumber(1);
          }}
        >
          <option>All status</option>
          {statuses.map((item) => (
            <option key={item}>{item}</option>
          ))}
        </select>
        <select aria-label="Sort order" value={sort} onChange={(e) => setSort(e.target.value)}>
          <option value="newest">Sort by: Newest</option>
          <option value="name">Sort by: Name</option>
        </select>
        <button className="button button--primary" onClick={() => startEdit()}>
          <Plus size={16} /> Add {name.replace(/s$/, "")}
        </button>
      </div>
      <div className="table-scroll">
        <table>
          <thead>
            <tr>
              {columns.map(([, title]) => (
                <th key={title}>{title}</th>
              ))}
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {visible.length ? (
              visible.map((row, i) => (
                <tr key={String(row.id ?? i)}>
                  {columns.map(([key]) => {
                    const value = String(row[key] ?? "—");
                    if (key === "status")
                      return (
                        <td key={key}>
                          <Status value={value} />
                        </td>
                      );
                    if (["name", "customer"].includes(key))
                      return (
                        <td key={key}>
                          <span className="person-cell">
                            <Avatar name={value} />
                            {value}
                          </span>
                        </td>
                      );
                    return <td key={key}>{value}</td>;
                  })}
                  <td>
                    <div className="row-actions">
                      <button title="View" onClick={() => setViewing(row)}>
                        <Eye size={15} />
                      </button>
                      <button title="Edit" onClick={() => startEdit(row)}>
                        <Pencil size={15} />
                      </button>
                      {name === "Payments" && row.receipt && (
                        <button
                          title="Download receipt"
                          onClick={() =>
                            downloadText(
                              `${String(row.receipt)}.txt`,
                              `CRSM OFFICIAL RECEIPT\n${String(row.receipt)}\nCustomer: ${String(row.customer)}\nRepair: ${String(row.repair)}\nAmount: ${String(row.amount)}\nStatus: ${String(row.status)}\n`,
                            )
                          }
                        >
                          <Download size={15} />
                        </button>
                      )}
                      <button
                        title="Deactivate"
                        className="delete"
                        onClick={() => setConfirming(row)}
                      >
                        <Trash2 size={15} />
                      </button>
                    </div>
                  </td>
                </tr>
              ))
            ) : (
              <tr>
                <td colSpan={columns.length + 1}>
                  <div className="empty-state">
                    <Search size={28} />
                    <b>No matching records</b>
                    <span>Try changing your search or filter.</span>
                  </div>
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
      <div className="pagination">
        <span>
          Showing {filtered.length ? (safePage - 1) * pageSize + 1 : 0} to{" "}
          {Math.min(safePage * pageSize, filtered.length)} of {filtered.length} {name.toLowerCase()}
        </span>
        <div>
          <button
            disabled={safePage === 1}
            onClick={() => setPageNumber((current) => Math.max(1, current - 1))}
          >
            <ChevronLeft size={15} />
          </button>
          {Array.from({ length: pageCount }, (_, index) => index + 1).map((item) => (
            <button
              className={safePage === item ? "current" : ""}
              onClick={() => setPageNumber(item)}
              key={item}
            >
              {item}
            </button>
          ))}
          <button
            disabled={safePage === pageCount}
            onClick={() => setPageNumber((current) => Math.min(pageCount, current + 1))}
          >
            <ChevronRight size={15} />
          </button>
        </div>
      </div>
      {(editing || viewing) && (
        <div className="modal-backdrop">
          <section className="record-dialog" role="dialog" aria-modal="true">
            <button
              className="modal-close"
              onClick={() => {
                setEditing(null);
                setViewing(null);
              }}
              aria-label="Close"
            >
              <X size={18} />
            </button>
            <ContentTitle
              icon={editing ? Pencil : Eye}
              title={`${editing ? "Edit" : "View"} ${name.replace(/s$/, "")}`}
              subtitle={
                editing
                  ? "Changes are saved to the shared demo data."
                  : "Record details from the shared CRSM workspace."
              }
            />
            <div className="record-form">
              {columns.map(([key, label]) => (
                <label className="field" key={key}>
                  <span>{label}</span>
                  <input
                    disabled={!editing || key === "id"}
                    value={String((editing || viewing)?.[key] ?? "")}
                    onChange={(event) =>
                      setEditing((current) =>
                        current ? { ...current, [key]: event.target.value } : current,
                      )
                    }
                  />
                </label>
              ))}
            </div>
            {error && <div className="form-error">{error}</div>}
            <div className="save-buttons">
              <button
                className="button button--ghost"
                onClick={() => {
                  setEditing(null);
                  setViewing(null);
                }}
              >
                Close
              </button>
              {editing && (
                <button className="button button--primary" onClick={submit}>
                  Save changes
                </button>
              )}
            </div>
          </section>
        </div>
      )}
      {confirming && (
        <div className="modal-backdrop">
          <section className="confirm-dialog" role="alertdialog" aria-modal="true">
            <span className="confirm-icon">
              <ShieldCheck size={24} />
            </span>
            <h3>Deactivate this record?</h3>
            <p>
              {String(
                confirming.name || confirming.customer || confirming.service || confirming.id,
              )}
              will be marked inactive and remain in the audit history.
            </p>
            <div className="save-buttons">
              <button className="button button--ghost" onClick={() => setConfirming(null)}>
                Cancel
              </button>
              <button
                className="button cancel-button"
                onClick={() => {
                  onDelete(confirming);
                  setConfirming(null);
                }}
              >
                Deactivate
              </button>
            </div>
          </section>
        </div>
      )}
    </section>
  );
}

function MiniBars() {
  return (
    <div className="mini-bars">
      {[42, 56, 45, 72, 61, 80, 74, 91, 68, 85, 97, 88].map((h, i) => (
        <span key={i} style={{ height: `${h}%` }} title={`Week ${i + 1}`} />
      ))}
    </div>
  );
}

function Dashboard({
  startWorkflow,
  repairStage,
  data,
  onAdvance,
  onOpen,
}: {
  startWorkflow: () => void;
  repairStage: number;
  data: DemoData;
  onAdvance: (patch?: Partial<DemoData["repair"]>) => void;
  onOpen: (page: Page) => void;
}) {
  return (
    <>
      <PageHeader
        page="Dashboard"
        action={
          <button className="button button--primary" onClick={startWorkflow}>
            <Plus size={16} />
            New repair workflow
          </button>
        }
      />
      <section className="welcome-banner">
        <div>
          <small>THURSDAY, OCTOBER 16</small>
          <h2>Good morning, Pablo.</h2>
          <p>Here’s what’s happening at your repair center today.</p>
        </div>
        <div className="welcome-art">
          <MonitorCog size={82} />
          <Wrench size={34} />
        </div>
      </section>
      <AdminActionCenter stage={repairStage} data={data} onAdvance={onAdvance} onOpen={onOpen} />
      <div className="summary-grid">
        <SummaryCard
          icon={CalendarDays}
          label="Today's Appointments"
          value={String(data.appointments.length)}
          trend={`${data.appointments.filter((item) => item.status === "Pending").length} awaiting confirmation`}
        />
        <SummaryCard
          icon={Wrench}
          label="Active Repairs"
          value={String(data.repairs.filter((item) => item.status !== "Completed").length)}
          trend={`${data.repairs.filter((item) => !item.technician).length} unassigned`}
          tone="orange"
        />
        <SummaryCard
          icon={Check}
          label="Ready for Pickup"
          value={String(
            data.repairs.filter((item) => item.status === "Ready for Pickup").length +
              (data.repair.stage === 6 ? 1 : 0),
          )}
          trend={data.repair.paid ? "Payment recorded" : "Payment pending"}
          tone="green"
        />
        <SummaryCard
          icon={CircleDollarSign}
          label="Revenue This Month"
          value={`₱${data.payments
            .filter((item) => item.status === "Paid")
            .reduce((sum, item) => sum + pesoValue(item.amount), 0)
            .toLocaleString("en-PH")}`}
          trend={`${data.payments.length} recorded payments`}
          tone="purple"
        />
      </div>
      <div className="dashboard-grid">
        <section className="panel chart-panel">
          <div className="panel-title">
            <div>
              <h3>Revenue overview</h3>
              <p>Payments received over the last 12 months</p>
            </div>
            <span className="legend">This year</span>
          </div>
          <div className="revenue-total">
            <span>₱184,620</span>
            <em>+12.4%</em>
          </div>
          <MiniBars />
          <div className="chart-labels">
            <span>Jan</span>
            <span>Mar</span>
            <span>May</span>
            <span>Jul</span>
            <span>Sep</span>
            <span>Nov</span>
          </div>
        </section>
        <section className="panel">
          <div className="panel-title">
            <div>
              <h3>Repair activity</h3>
              <p>Current repair pipeline</p>
            </div>
            <MoreHorizontal size={20} />
          </div>
          <div className="activity-list">
            {[
              ["Diagnosing", 6, 30],
              ["In progress", 18, 78],
              ["Waiting for parts", 4, 22],
              ["Ready for pickup", 7, 42],
            ].map(([label, value, width]) => (
              <div key={String(label)}>
                <span>
                  <b>{label}</b>
                  <strong>{value}</strong>
                </span>
                <i>
                  <em style={{ width: `${width}%` }} />
                </i>
              </div>
            ))}
          </div>
        </section>
      </div>
      <section className="panel recent-panel">
        <div className="panel-title">
          <div>
            <h3>Recent appointments</h3>
            <p>Today’s upcoming customer visits</p>
          </div>
          <button className="text-button" onClick={() => onOpen("Appointments")}>
            View all <ChevronRight size={14} />
          </button>
        </div>
        <div className="appointment-list">
          {appointments.slice(0, 4).map((a) => (
            <div key={a.id}>
              <span className="time-box">{a.date.split("·")[1]}</span>
              <Avatar name={a.customer} />
              <span className="appointment-main">
                <b>{a.customer}</b>
                <small>
                  {a.service} · {a.device}
                </small>
              </span>
              <span className="tech-name">{a.technician}</span>
              <Status value={a.status} />
            </div>
          ))}
        </div>
      </section>
    </>
  );
}

const listConfigs: Partial<
  Record<
    Page,
    {
      cards: [IconType, string, string, string, string][];
      rows: Row[];
      columns: [string, string][];
      noun: string;
    }
  >
> = {
  Customers: {
    cards: [
      [Users, "Total Customers", "248", "+12% this month", "blue"],
      [User, "New Customers", "18", "+20% from last month", "green"],
      [ShieldCheck, "Active Customers", "195", "+8% this month", "orange"],
      [Users, "Inactive Customers", "53", "-6% from last month", "red"],
    ],
    rows: people,
    columns: [
      ["id", "Customer ID"],
      ["name", "Customer Name"],
      ["phone", "Contact Number"],
      ["email", "Email Address"],
      ["address", "Address"],
      ["devices", "Devices"],
      ["repairs", "Repairs"],
      ["status", "Status"],
      ["added", "Date Added"],
    ],
    noun: "Customers",
  },
  Computers: {
    cards: [
      [Computer, "Total Devices", "312", "+8% this month", "blue"],
      [Plus, "New Devices", "28", "+27% from last month", "green"],
      [Wrench, "In for Repair", "46", "+12% from last month", "orange"],
      [Check, "Completed Repairs", "212", "+15% this month", "green"],
    ],
    rows: computers,
    columns: [
      ["id", "Device ID"],
      ["customer", "Customer Name"],
      ["type", "Device Type"],
      ["brand", "Brand"],
      ["model", "Model"],
      ["serial", "Serial Number"],
      ["status", "Status"],
      ["added", "Date Added"],
    ],
    noun: "Devices",
  },
  Appointments: {
    cards: [
      [CalendarDays, "Total Appointments", "48", "+12% this month", "blue"],
      [Check, "Confirmed", "28", "+20% from last month", "green"],
      [Clock3, "Pending", "12", "+3% from last month", "orange"],
      [X, "Cancelled", "8", "-11% from last month", "red"],
    ],
    rows: appointments,
    columns: [
      ["id", "Appointment ID"],
      ["date", "Date & Time"],
      ["customer", "Customer Name"],
      ["device", "Device"],
      ["service", "Service Type"],
      ["technician", "Technician"],
      ["status", "Status"],
      ["notes", "Notes"],
    ],
    noun: "Appointments",
  },
  "Repair Requests": {
    cards: [
      [FileText, "Total Requests", "184", "+16 this month", "blue"],
      [Activity, "In Progress", "18", "6 due today", "orange"],
      [HardDrive, "Waiting for Parts", "4", "2 delayed", "red"],
      [Check, "Completed", "162", "+14% this month", "green"],
    ],
    rows: repairs,
    columns: [
      ["id", "Request ID"],
      ["customer", "Customer"],
      ["device", "Computer"],
      ["issue", "Reported Issue"],
      ["technician", "Technician"],
      ["cost", "Estimate"],
      ["status", "Status"],
      ["updated", "Last Updated"],
    ],
    noun: "Repair Requests",
  },
  Services: {
    cards: [
      [Wrench, "Total Services", "24", "6 categories", "blue"],
      [Check, "Active Services", "21", "87.5% of total", "green"],
      [Clock3, "Average Duration", "2.3 hrs", "-8% this month", "orange"],
      [CircleDollarSign, "Average Price", "₱950", "+4.2% this month", "purple"],
    ],
    rows: services,
    columns: [
      ["id", "Service ID"],
      ["service", "Service Name"],
      ["description", "Description"],
      ["category", "Category"],
      ["duration", "Est. Duration"],
      ["price", "Base Price"],
      ["status", "Status"],
    ],
    noun: "Services",
  },
  Technicians: {
    cards: [
      [Users, "Total Technicians", "8", "All staff", "blue"],
      [User, "Active Technicians", "8", "100% available", "green"],
      [Wrench, "Current Assignments", "18", "2.3 avg. workload", "orange"],
      [Clock3, "On Leave", "0", "Full capacity", "red"],
    ],
    rows: technicians,
    columns: [
      ["id", "Technician ID"],
      ["name", "Name"],
      ["phone", "Contact Number"],
      ["email", "Email Address"],
      ["specialization", "Specialization / Skills"],
      ["status", "Status"],
      ["assignments", "Current Assignments"],
    ],
    noun: "Technicians",
  },
  Payments: {
    cards: [
      [CircleDollarSign, "Collected This Month", "₱184,620", "+12.4% from September", "blue"],
      [Check, "Paid Invoices", "142", "94.2% payment rate", "green"],
      [Clock3, "Pending Balance", "₱28,450", "18 open invoices", "orange"],
      [FileText, "Transactions", "160", "+21 this month", "purple"],
    ],
    rows: payments,
    columns: [
      ["id", "Payment ID"],
      ["date", "Date"],
      ["customer", "Customer"],
      ["repair", "Repair Request"],
      ["method", "Method"],
      ["amount", "Amount"],
      ["status", "Status"],
      ["receipt", "Receipt No."],
    ],
    noun: "Payments",
  },
  "User Accounts": {
    cards: [
      [Users, "Total Users", "12", "3 user roles", "blue"],
      [User, "Active Users", "12", "All accounts enabled", "green"],
      [ShieldCheck, "Administrators", "1", "Full access", "red"],
      [UserCog, "User Roles", "3", "Admin, technician, staff", "purple"],
    ],
    rows: users,
    columns: [
      ["id", "User ID"],
      ["name", "Full Name"],
      ["username", "Username"],
      ["email", "Email Address"],
      ["role", "Role"],
      ["status", "Status"],
      ["created", "Date Created"],
      ["login", "Last Login"],
    ],
    noun: "Users",
  },
};

const pageDataKeys: Partial<Record<Page, keyof DemoData>> = {
  Customers: "customers",
  Computers: "computers",
  Appointments: "appointments",
  "Repair Requests": "repairs",
  Services: "services",
  Technicians: "technicians",
  Payments: "payments",
  "User Accounts": "users",
};

function ListPage({
  page,
  data,
  update,
  notify,
}: {
  page: Page;
  data: DemoData;
  update: (updater: (current: DemoData) => DemoData) => void;
  notify: (message: string) => void;
}) {
  const config = listConfigs[page]!;
  const dataKey = pageDataKeys[page]!;
  const rows = data[dataKey] as Row[];
  const save = (row: Row) => {
    update((current) => {
      const collection = current[dataKey] as Row[];
      const exists = collection.some((item) => String(item.id) === String(row.id));
      return {
        ...current,
        [dataKey]: exists
          ? collection.map((item) => (String(item.id) === String(row.id) ? row : item))
          : [row, ...collection],
      };
    });
    notify(`${config.noun.replace(/s$/, "")} saved to the shared workspace.`);
  };
  const deactivate = (row: Row) => {
    update((current) => ({
      ...current,
      [dataKey]: (current[dataKey] as Row[]).map((item) =>
        String(item.id) === String(row.id) ? { ...item, status: "Inactive" } : item,
      ),
    }));
    notify("Record deactivated. It remains available in the audit history.");
  };
  return (
    <>
      <PageHeader page={page} />
      <div className="summary-grid">
        {config.cards.map(([icon, label, value, trend, tone]) => (
          <SummaryCard
            key={label}
            icon={icon}
            label={label}
            value={value}
            trend={trend}
            tone={tone}
          />
        ))}
      </div>
      <TableView
        rows={rows}
        columns={config.columns}
        name={config.noun}
        onSave={save}
        onDelete={deactivate}
      />
    </>
  );
}

function downloadText(filename: string, content: string, type = "text/plain") {
  const blob = new Blob([content], { type });
  const url = URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url;
  link.download = filename;
  link.click();
  URL.revokeObjectURL(url);
}

function pesoValue(value: string | number) {
  return Number(String(value).replace(/[^\d.]/g, "")) || 0;
}

function Reports({ data, notify }: { data: DemoData; notify: (message: string) => void }) {
  const [from, setFrom] = useState("2026-10-01");
  const [to, setTo] = useState("2026-10-31");
  const [applied, setApplied] = useState({ from, to });
  const filteredPayments = data.payments.filter((payment) => {
    const parsed = new Date(String(payment.date));
    if (Number.isNaN(parsed.valueOf())) return true;
    return parsed >= new Date(applied.from) && parsed <= new Date(`${applied.to}T23:59:59`);
  });
  const revenue = filteredPayments
    .filter((payment) => String(payment.status) === "Paid")
    .reduce((sum, payment) => sum + pesoValue(payment.amount), 0);
  const exportReport = () => {
    const header = "Payment ID,Date,Customer,Repair,Method,Amount,Status,Receipt";
    const lines = filteredPayments.map((payment) =>
      ["id", "date", "customer", "repair", "method", "amount", "status", "receipt"]
        .map((key) => `"${String(payment[key] ?? "").replace(/"/g, '""')}"`)
        .join(","),
    );
    downloadText("crsm-report.csv", [header, ...lines].join("\n"), "text/csv");
    notify("Filtered report downloaded as CSV.");
  };
  return (
    <>
      <PageHeader
        page="Reports"
        action={
          <button className="button button--primary" onClick={exportReport}>
            <Download size={16} />
            Export results
          </button>
        }
      />
      <section className="filter-strip">
        <label>
          From
          <input type="date" value={from} onChange={(event) => setFrom(event.target.value)} />
        </label>
        <label>
          To
          <input type="date" value={to} onChange={(event) => setTo(event.target.value)} />
        </label>
        <button
          className="button button--primary"
          onClick={() => {
            if (new Date(from) > new Date(to)) {
              notify("The report start date must be before the end date.");
              return;
            }
            setApplied({ from, to });
            notify(`Report updated with ${filteredPayments.length} payment records.`);
          }}
        >
          Apply filters
        </button>
      </section>
      <div className="summary-grid">
        <SummaryCard
          icon={CircleDollarSign}
          label="Total Revenue"
          value={`₱${revenue.toLocaleString("en-PH", { minimumFractionDigits: 2 })}`}
          trend={`${filteredPayments.length} transactions in range`}
        />
        <SummaryCard
          icon={FileText}
          label="Repairs Completed"
          value={String(data.repairs.filter((repair) => repair.status === "Completed").length)}
          trend="Based on current repair records"
          tone="green"
        />
        <SummaryCard
          icon={Users}
          label="New Customers"
          value={String(data.customers.length)}
          trend="Total customer records"
          tone="orange"
        />
        <SummaryCard
          icon={Clock3}
          label="Avg. Turnaround"
          value="2.6 days"
          trend="-0.4 days faster"
          tone="purple"
        />
      </div>
      <div className="dashboard-grid">
        <section className="panel chart-panel">
          <div className="panel-title">
            <div>
              <h3>Revenue performance</h3>
              <p>Daily collections for the selected period</p>
            </div>
            <span className="legend">Revenue</span>
          </div>
          <div className="revenue-total">
            <span>₱{revenue.toLocaleString("en-PH")}</span>
            <em>+12.4%</em>
          </div>
          <MiniBars />
          <div className="chart-labels">
            <span>Oct 1</span>
            <span>Oct 4</span>
            <span>Oct 8</span>
            <span>Oct 12</span>
            <span>Oct 16</span>
          </div>
        </section>
        <section className="panel">
          <div className="panel-title">
            <div>
              <h3>Services breakdown</h3>
              <p>Most requested services</p>
            </div>
          </div>
          <div className="service-chart">
            {[
              ["Hardware Repair", 38],
              ["OS Reinstallation", 24],
              ["Diagnostics", 18],
              ["Malware Removal", 12],
              ["Other services", 8],
            ].map(([n, v]) => (
              <div key={n}>
                <span>
                  <b>{n}</b>
                  <em>{v}%</em>
                </span>
                <i>
                  <em style={{ width: `${v}%` }} />
                </i>
              </div>
            ))}
          </div>
        </section>
      </div>
    </>
  );
}

function SettingsNav({ page, onPage }: { page: Page; onPage: (p: Page) => void }) {
  return (
    <aside className="settings-nav">
      <div className="settings-user">
        <Avatar name="Pablo Job" size="lg" />
        <b>Pablo Job</b>
        <span>Administrator</span>
        <em>
          <i /> Active
        </em>
      </div>
      {(["My Profile", "Account Settings", "Change Password", "Login Activity"] as Page[]).map(
        (p) => (
          <button className={page === p ? "active" : ""} onClick={() => onPage(p)} key={p}>
            {p === "My Profile" ? (
              <User size={16} />
            ) : p === "Account Settings" ? (
              <Settings size={16} />
            ) : p === "Change Password" ? (
              <KeyRound size={16} />
            ) : (
              <Activity size={16} />
            )}
            {p}
          </button>
        ),
      )}
    </aside>
  );
}

function Toggle({ initial = true }: { initial?: boolean }) {
  const [on, setOn] = useState(initial);
  return (
    <button
      aria-label="Toggle setting"
      className={`toggle ${on ? "on" : ""}`}
      onClick={() => setOn(!on)}
    >
      <span />
    </button>
  );
}

function SettingsPage({ notify }: { notify: (message: string) => void }) {
  const [activeTab, setActiveTab] = useState("General");
  return (
    <>
      <PageHeader page="Settings" />
      <div className="settings-tabs">
        <button
          className={activeTab === "General" ? "active" : ""}
          onClick={() => setActiveTab("General")}
        >
          <Settings size={15} />
          General
        </button>
        <button
          className={activeTab === "Business" ? "active" : ""}
          onClick={() => setActiveTab("Business")}
        >
          <FileText size={15} />
          Business Information
        </button>
        <button
          className={activeTab === "Pricing" ? "active" : ""}
          onClick={() => setActiveTab("Pricing")}
        >
          <Wrench size={15} />
          Service & Pricing
        </button>
        <button
          className={activeTab === "Appointments" ? "active" : ""}
          onClick={() => setActiveTab("Appointments")}
        >
          <CalendarDays size={15} />
          Appointments
        </button>
        <button
          className={activeTab === "Notifications" ? "active" : ""}
          onClick={() => setActiveTab("Notifications")}
        >
          <Bell size={15} />
          Notifications
        </button>
      </div>
      <div className="settings-cards">
        <SettingsCard
          onSave={() => notify("General settings saved.")}
          icon={Settings}
          title="General Settings"
          subtitle="Configure basic system preferences."
        >
          <Field label="System Name" value="Computer Repair Service Management (CRSM)" />
          <Field label="Timezone" value="GMT+08:00 Philippine Time (Manila)" />
          <Field label="Currency Symbol" value="₱" />
        </SettingsCard>
        <SettingsCard
          onSave={() => notify("Business information saved.")}
          icon={FileText}
          title="Business Information"
          subtitle="Update your shop's business details."
        >
          <Field label="Shop Name" value="Computer Repair Service Management" />
          <Field label="Address" value="Batac City, Ilocos Norte, Philippines" />
          <Field label="Business Email" value="crsm@repairshop.com" />
        </SettingsCard>
        <SettingsCard
          onSave={() => notify("Service and pricing defaults saved.")}
          icon={Wrench}
          title="Service & Pricing"
          subtitle="Configure default settings for services and pricing."
        >
          <Field label="Default Labor Rate (per hour)" value="₱500.00" />
          <Field label="Default Diagnostic Fee" value="₱300.00" />
          <SettingLine label="Require approval for price changes">
            <Toggle />
          </SettingLine>
        </SettingsCard>
        <SettingsCard
          onSave={() => notify("Appointment settings saved.")}
          icon={CalendarDays}
          title="Appointment Settings"
          subtitle="Configure appointment preferences."
        >
          <Field label="Default Appointment Duration" value="1 Hour" />
          <Field label="Advance Booking Limit" value="30 days" />
          <SettingLine label="Allow online appointments">
            <Toggle />
          </SettingLine>
        </SettingsCard>
        <SettingsCard
          onSave={() => notify("Notification settings saved.")}
          icon={Bell}
          title="Notification Settings"
          subtitle="Manage system notifications."
        >
          {[
            "Appointment reminders",
            "Repair status updates",
            "Payment confirmations",
            "Low stock alerts",
          ].map((v, i) => (
            <SettingLine label={v} key={v}>
              <Toggle initial={i < 3} />
            </SettingLine>
          ))}
        </SettingsCard>
        <SettingsCard
          onSave={() => notify("System settings saved.")}
          icon={ShieldCheck}
          title="System Settings"
          subtitle="Configure system behavior and security."
        >
          <SettingLine label="Require login authentication">
            <Toggle />
          </SettingLine>
          <SettingLine label="Auto-logout after inactivity">
            <Toggle />
          </SettingLine>
          <SettingLine label="Enable activity logging">
            <Toggle />
          </SettingLine>
        </SettingsCard>
      </div>
    </>
  );
}

function SettingsCard({
  icon: Icon,
  title,
  subtitle,
  children,
  onSave,
}: {
  icon: IconType;
  title: string;
  subtitle: string;
  children: ReactNode;
  onSave: () => void;
}) {
  return (
    <section className="settings-card">
      <div className="settings-card-title">
        <Icon size={20} />
        <div>
          <h3>{title}</h3>
          <p>{subtitle}</p>
        </div>
      </div>
      <div className="settings-fields">{children}</div>
      <button className="button button--primary save-card" onClick={onSave}>
        Save Changes
      </button>
    </section>
  );
}

function Field({ label, value, type = "text" }: { label: string; value: string; type?: string }) {
  return (
    <label className="field">
      <span>{label}</span>
      <input type={type} defaultValue={value} />
    </label>
  );
}

function SettingLine({ label, children }: { label: string; children: ReactNode }) {
  return (
    <div className="setting-line">
      <span>{label}</span>
      {children}
    </div>
  );
}

function AccountPage({
  page,
  onPage,
  notify,
}: {
  page: Page;
  onPage: (p: Page) => void;
  notify: (message: string) => void;
}) {
  return (
    <>
      <PageHeader page={page} />
      <div className="account-layout">
        <SettingsNav page={page} onPage={onPage} />
        <section className="account-content">
          {page === "My Profile" && (
            <>
              <ContentTitle
                icon={User}
                title="Profile Information"
                subtitle="Update your personal details and contact information."
              />
              <div className="form-grid">
                <Field label="Full Name" value="Pablo Job" />
                <Field label="Email Address" value="pablo.job@crsm.com" />
                <Field label="Contact Number" value="0917 555 0142" />
                <Field label="Role" value="Administrator" />
                <label className="field field--wide">
                  <span>Address</span>
                  <textarea defaultValue="Batac City, Ilocos Norte, Philippines" />
                </label>
              </div>
              <SaveButtons
                onSave={() => notify("Profile information saved.")}
                onCancel={() => notify("Profile changes discarded.")}
              />
            </>
          )}
          {page === "Account Settings" && (
            <>
              <ContentTitle
                icon={SlidersHorizontal}
                title="Personal Preferences"
                subtitle="Customize your experience in the system."
              />
              <div className="form-grid">
                <Field label="Language" value="English (Philippines)" />
                <Field label="Timezone" value="GMT+08:00 Philippine Time" />
                <Field label="Date Format" value="Oct 16, 2026" />
                <Field label="Time Format" value="12-hour (AM/PM)" />
              </div>
              <div className="preferences">
                <h3>Notification Preferences</h3>
                {[
                  "Appointment reminders",
                  "Repair status updates",
                  "Payment confirmations",
                  "System announcements",
                ].map((v) => (
                  <SettingLine key={v} label={v}>
                    <Toggle />
                  </SettingLine>
                ))}
              </div>
              <SaveButtons
                onSave={() => notify("Account preferences saved.")}
                onCancel={() => notify("Preference changes discarded.")}
              />
            </>
          )}
          {page === "Change Password" && (
            <>
              <ContentTitle
                icon={KeyRound}
                title="Change Password"
                subtitle="Set a new password for your account."
              />
              <div className="password-layout">
                <div>
                  <PasswordField
                    label="Current Password"
                    placeholder="Enter your current password"
                  />
                  <PasswordField label="New Password" placeholder="Enter your new password" />
                  <PasswordField
                    label="Confirm New Password"
                    placeholder="Confirm your new password"
                  />
                  <SaveButtons
                    onSave={() => notify("Password updated successfully.")}
                    onCancel={() => notify("Password change cancelled.")}
                  />
                </div>
                <aside className="requirements">
                  <ShieldCheck size={24} />
                  <h3>Password Requirements</h3>
                  {[
                    "At least 8 characters long",
                    "At least one uppercase letter",
                    "At least one lowercase letter",
                    "At least one number",
                    "At least one special character",
                  ].map((v) => (
                    <span key={v}>
                      <i />
                      {v}
                    </span>
                  ))}
                </aside>
              </div>
            </>
          )}
          {page === "Login Activity" && (
            <>
              <ContentTitle
                icon={Activity}
                title="Login Activity"
                subtitle="Recent sign-ins and currently active sessions."
              />
              <div className="login-list">
                {[
                  ["Batac City, Philippines", "Chrome on Windows", "Current session · 10:24 AM"],
                  ["Laoag City, Philippines", "Safari on iPhone", "Oct 15, 2026 · 6:42 PM"],
                  ["Batac City, Philippines", "Chrome on Windows", "Oct 14, 2026 · 8:04 AM"],
                ].map(([place, device, date], i) => (
                  <div key={date}>
                    <span className="login-icon">
                      <MonitorCog size={20} />
                    </span>
                    <span>
                      <b>{device}</b>
                      <small>{place}</small>
                    </span>
                    <em className={i === 0 ? "current-session" : ""}>{date}</em>
                    {i > 0 && (
                      <button
                        className="text-button"
                        onClick={() => notify(`Signed out the ${device} session.`)}
                      >
                        Sign out
                      </button>
                    )}
                  </div>
                ))}
              </div>
            </>
          )}
        </section>
      </div>
    </>
  );
}

function ContentTitle({
  icon: Icon,
  title,
  subtitle,
}: {
  icon: IconType;
  title: string;
  subtitle: string;
}) {
  return (
    <div className="content-title">
      <Icon size={24} />
      <div>
        <h2>{title}</h2>
        <p>{subtitle}</p>
      </div>
    </div>
  );
}

function SaveButtons({ onSave, onCancel }: { onSave: () => void; onCancel: () => void }) {
  return (
    <div className="save-buttons">
      <button className="button button--ghost" onClick={onCancel}>
        Cancel
      </button>
      <button className="button button--primary" onClick={onSave}>
        Save Changes
      </button>
    </div>
  );
}

function PasswordField({ label, placeholder }: { label: string; placeholder: string }) {
  const [show, setShow] = useState(false);
  return (
    <label className="field password-field">
      <span>{label}</span>
      <div>
        <KeyRound size={15} />
        <input type={show ? "text" : "password"} placeholder={placeholder} />
        <button onClick={() => setShow(!show)} type="button">
          {show ? <EyeOff size={16} /> : <Eye size={16} />}
        </button>
      </div>
    </label>
  );
}

const workflowSteps = [
  { title: "Find customer", description: "Search the customer directory", icon: Users },
  { title: "Select computer", description: "Choose a registered device", icon: Laptop },
  { title: "Book appointment", description: "Reserve a service time", icon: CalendarDays },
  { title: "Create repair request", description: "Capture the reported issue", icon: FileText },
  { title: "Assign technician", description: "Choose an available specialist", icon: UserCog },
  { title: "Update repair status", description: "Move the work into progress", icon: Wrench },
  { title: "Record payment", description: "Complete the service workflow", icon: CircleDollarSign },
];

function WorkflowModal({ onClose, onDone }: { onClose: () => void; onDone: () => void }) {
  const [step, setStep] = useState(0);
  const current = workflowSteps[step];
  const Icon = current.icon;
  return (
    <div className="modal-backdrop">
      <section
        className="workflow-modal"
        role="dialog"
        aria-modal="true"
        aria-label="New repair workflow"
      >
        <button className="modal-close" onClick={onClose}>
          <X size={20} />
        </button>
        <div className="workflow-sidebar">
          <Logo compact />
          <p>New repair workflow</p>
          <div className="workflow-steps">
            {workflowSteps.map((s, i) => (
              <div key={s.title} className={i === step ? "active" : i < step ? "done" : ""}>
                <span>{i < step ? <Check size={14} /> : i + 1}</span>
                <div>
                  <b>{s.title}</b>
                  <small>{s.description}</small>
                </div>
              </div>
            ))}
          </div>
        </div>
        <div className="workflow-content">
          <div className="workflow-heading">
            <span>
              <Icon size={23} />
            </span>
            <div>
              <small>
                STEP {step + 1} OF {workflowSteps.length}
              </small>
              <h2>{current.title}</h2>
              <p>{current.description} for Ara Bernabe’s ACER laptop.</p>
            </div>
          </div>
          <div className="workflow-form">
            {step === 0 && (
              <>
                <label className="search-field large">
                  <Search size={16} />
                  <input defaultValue="Ara Bernabe" />
                </label>
                <div className="select-card selected">
                  <Avatar name="Ara Bernabe" />
                  <span>
                    <b>Ara Bernabe</b>
                    <small>C-001 · 0927 123 4567 · 2 devices</small>
                  </span>
                  <Check size={18} />
                </div>
              </>
            )}
            {step === 1 && (
              <>
                <div className="select-card selected">
                  <Laptop size={28} />
                  <span>
                    <b>ACER Predator Helios Neo 16</b>
                    <small>D-001 · Serial: ACERPH00123</small>
                  </span>
                  <Check size={18} />
                </div>
                <div className="select-card">
                  <Computer size={28} />
                  <span>
                    <b>ACER Aspire TC</b>
                    <small>D-011 · Serial: ACTC55198</small>
                  </span>
                </div>
              </>
            )}
            {step === 2 && (
              <div className="form-grid">
                <Field label="Appointment Date" value="2026-10-17" type="date" />
                <Field label="Time" value="10:00 AM" />
                <Field label="Service" value="Hardware Diagnostics" />
                <Field label="Duration" value="1 hour" />
              </div>
            )}
            {step === 3 && (
              <>
                <label className="field">
                  <span>Reported issue</span>
                  <textarea defaultValue="Laptop powers on but the screen remains black after startup." />
                </label>
                <div className="form-grid">
                  <Field label="Priority" value="Normal" />
                  <Field label="Initial Estimate" value="₱4,850.00" />
                </div>
              </>
            )}
            {step === 4 && (
              <>
                {technicians.slice(0, 3).map((t, i) => (
                  <div className={`select-card ${i === 1 ? "selected" : ""}`} key={t.id}>
                    <Avatar name={t.name} />
                    <span>
                      <b>{t.name}</b>
                      <small>
                        {t.specialization} · {t.assignments} active jobs
                      </small>
                    </span>
                    {i === 1 && <Check size={18} />}
                  </div>
                ))}
              </>
            )}
            {step === 5 && (
              <>
                <div className="status-flow">
                  <Status value="Diagnosing" />
                  <ChevronRight size={18} />
                  <Status value="In Progress" />
                </div>
                <label className="field">
                  <span>Repair notes</span>
                  <textarea defaultValue="Display cable tested. Replacement panel ordered; repair is now in progress." />
                </label>
              </>
            )}
            {step === 6 && (
              <>
                <div className="payment-total">
                  <span>Total due</span>
                  <strong>₱4,850.00</strong>
                  <small>Repair request R-1024</small>
                </div>
                <div className="form-grid">
                  <Field label="Payment Method" value="GCash" />
                  <Field label="Reference Number" value="GC-161026-184" />
                </div>
                <div className="success-note">
                  <Check size={18} /> Receipt OR-0184 will be generated automatically.
                </div>
              </>
            )}
          </div>
          <div className="workflow-actions">
            <button
              className="button button--ghost"
              onClick={() => (step ? setStep(step - 1) : onClose())}
            >
              {step ? "Back" : "Cancel"}
            </button>
            <button
              className="button button--primary"
              onClick={() => (step < workflowSteps.length - 1 ? setStep(step + 1) : onDone())}
            >
              {step === workflowSteps.length - 1 ? "Record payment & finish" : "Continue"}
              <ChevronRight size={16} />
            </button>
          </div>
        </div>
      </section>
    </div>
  );
}

function AdminActionCenter({
  stage,
  data,
  onAdvance,
  onOpen,
}: {
  stage: number;
  data: DemoData;
  onAdvance: (patch?: Partial<DemoData["repair"]>) => void;
  onOpen: (page: Page) => void;
}) {
  const [checkinOpen, setCheckinOpen] = useState(false);
  const [condition, setCondition] = useState(
    data.repair.condition || "Minor scratch on lower-left lid",
  );
  const [accessories, setAccessories] = useState(
    data.repair.accessories || "180W charger, laptop sleeve",
  );
  const [technician, setTechnician] = useState(data.repair.technician || "Vina Saji Fukiko");
  const [error, setError] = useState("");
  const adminActions: Record<
    number,
    { title: string; copy: string; action: string; icon: IconType }
  > = {
    0: {
      title: "Appointment awaiting confirmation",
      copy: "Ara Bernabe requested an Oct 17 drop-off for ACER Predator Helios Neo 16.",
      action: "Review & confirm booking",
      icon: CalendarDays,
    },
    1: {
      title: "Device ready for check-in",
      copy: "Record device condition and accessories, then confirm the technician assignment.",
      action: "Check in & assign",
      icon: Computer,
    },
    5: {
      title: "Technical work complete",
      copy: "Review the final charge, record payment, and issue receipt OR-0184.",
      action: "Record ₱4,850 & mark ready",
      icon: CircleDollarSign,
    },
    6: {
      title: "Device awaiting pickup",
      copy: "Payment is complete and Ara has been notified. Log collection when released.",
      action: "Log device collection",
      icon: PackageCheck,
    },
  };
  const item = adminActions[stage];
  const responsible =
    stage === 2
      ? "Technician is responsible for diagnosis"
      : stage === 3
        ? "Customer is reviewing the estimate"
        : stage === 4
          ? "Technician is completing approved work"
          : stage === 7
            ? "Workflow complete"
            : "";
  return (
    <>
      <section className="handoff-card handoff-card--admin">
        <div className="handoff-copy">
          <span className="handoff-icon">
            {item ? <item.icon size={22} /> : <Activity size={22} />}
          </span>
          <div>
            <small>LIVE HANDOFF · REPAIR R-1024</small>
            <h3>{item?.title || repairStages[stage]}</h3>
            <p>
              {item?.copy ||
                `${responsible}. The dashboard will update when their next action is complete.`}
            </p>
          </div>
        </div>
        {stage === 1 && (
          <div className="checkin-preview">
            <span>
              <b>Condition</b> Minor scratch on lower-left lid
            </span>
            <span>
              <b>Accessories</b> 180W charger, laptop sleeve
            </span>
            <span>
              <b>Assigned to</b> Vina Saji Fukiko
            </span>
          </div>
        )}
        {item && (
          <button
            className="button button--primary"
            onClick={() => (stage === 1 ? setCheckinOpen(true) : onAdvance())}
          >
            {item.action} <ChevronRight size={16} />
          </button>
        )}
        {!item && <Status value={stage === 7 ? "Completed" : "In Progress"} />}
      </section>
      <div className="admin-queue-strip">
        <button onClick={() => onOpen("Appointments")}>
          <CalendarDays size={18} />
          <span>
            <b>
              {data.appointments.filter((item) => item.status === "Pending").length} awaiting
              confirmation
            </b>
            <small>Review requested bookings</small>
          </span>
          <ChevronRight size={15} />
        </button>
        <button onClick={() => onOpen("Repair Requests")}>
          <UserCog size={18} />
          <span>
            <b>
              {data.repairs.filter((item) => !String(item.technician || "")).length} unassigned
              repairs
            </b>
            <small>Review technician availability</small>
          </span>
          <ChevronRight size={15} />
        </button>
        <button onClick={() => onOpen("Payments")}>
          <PackageCheck size={18} />
          <span>
            <b>
              {data.repairs.filter((item) => item.status === "Ready for Pickup").length +
                (data.repair.stage === 6 ? 1 : 0)}{" "}
              awaiting pickup
            </b>
            <small>₱18,250 outstanding</small>
          </span>
          <ChevronRight size={15} />
        </button>
      </div>
      {checkinOpen && (
        <div className="modal-backdrop">
          <section className="record-dialog" role="dialog" aria-modal="true">
            <button className="modal-close" onClick={() => setCheckinOpen(false)}>
              <X size={18} />
            </button>
            <ContentTitle
              icon={Computer}
              title="Check in device"
              subtitle="Record intake details and assign an available technician."
            />
            <label className="field">
              <span>Device condition</span>
              <textarea value={condition} onChange={(event) => setCondition(event.target.value)} />
            </label>
            <label className="field">
              <span>Accessories received</span>
              <input value={accessories} onChange={(event) => setAccessories(event.target.value)} />
            </label>
            <label className="field">
              <span>Assign technician</span>
              <select value={technician} onChange={(event) => setTechnician(event.target.value)}>
                {data.technicians
                  .filter((item) => item.status === "Active")
                  .map((item) => (
                    <option key={String(item.id)} value={String(item.name)}>
                      {String(item.name)} · {String(item.assignments)} active jobs
                    </option>
                  ))}
              </select>
            </label>
            {error && <div className="form-error">{error}</div>}
            <div className="save-buttons">
              <button className="button button--ghost" onClick={() => setCheckinOpen(false)}>
                Cancel
              </button>
              <button
                className="button button--primary"
                onClick={() => {
                  if (!condition.trim() || !accessories.trim() || !technician) {
                    setError("Condition, accessories, and technician are required.");
                    return;
                  }
                  onAdvance({ condition, accessories, technician });
                  setCheckinOpen(false);
                }}
              >
                Check in & assign
              </button>
            </div>
          </section>
        </div>
      )}
    </>
  );
}

const customerNav: { label: CustomerPage; icon: IconType }[] = [
  { label: "Home", icon: Gauge },
  { label: "My Computers", icon: Computer },
  { label: "Appointments", icon: CalendarDays },
  { label: "My Repairs", icon: Wrench },
  { label: "Services & Pricing", icon: FileText },
  { label: "Payments & Receipts", icon: CircleDollarSign },
  { label: "Profile", icon: User },
];

const technicianNav: { label: TechnicianPage; icon: IconType }[] = [
  { label: "My Assigned Repairs", icon: Wrench },
  { label: "My Schedule", icon: CalendarDays },
  { label: "Profile", icon: User },
];

function PortalSidebar<P extends string>({
  role,
  page,
  onPage,
  open,
  onClose,
  onLogout,
}: {
  role: "Customer" | "Technician";
  page: P;
  onPage: (page: P) => void;
  open: boolean;
  onClose: () => void;
  onLogout: () => void;
}) {
  const items = (role === "Customer" ? customerNav : technicianNav) as {
    label: P;
    icon: IconType;
  }[];
  return (
    <aside className={`sidebar portal-sidebar ${open ? "sidebar--open" : ""}`}>
      <button className="mobile-close" onClick={onClose} aria-label="Close menu">
        <X size={20} />
      </button>
      <Logo />
      <div className="portal-role">
        <span>{role === "Customer" ? "CUSTOMER PORTAL" : "TECHNICIAN WORKSPACE"}</span>
      </div>
      <nav>
        <div className="nav-group">
          {items.map(({ label, icon: Icon }) => (
            <button
              key={label}
              onClick={() => {
                onPage(label);
                onClose();
              }}
              className={page === label ? "active" : ""}
            >
              <Icon size={18} />
              <span>{label}</span>
            </button>
          ))}
        </div>
      </nav>
      <div className="sidebar-watermark">
        <MonitorCog size={62} />
        <strong>CRSM</strong>
      </div>
      <button className="logout" onClick={onLogout}>
        <LogOut size={17} /> Logout
      </button>
    </aside>
  );
}

function PortalTopbar({
  role,
  onMenu,
  onSignOut,
  onSearch,
}: {
  role: "Customer" | "Technician";
  onMenu: () => void;
  onSignOut: () => void;
  onSearch: () => void;
}) {
  const name = role === "Customer" ? "Ara Bernabe" : "Vina Saji Fukiko";
  const [search, setSearch] = useState("");
  return (
    <header className="topbar">
      <button className="menu-button" onClick={onMenu} aria-label="Open menu">
        <Menu size={20} />
      </button>
      <label className="global-search">
        <Search size={15} />
        <input
          aria-label="Search"
          placeholder={role === "Customer" ? "Search your repairs..." : "Search assigned jobs..."}
          value={search}
          onChange={(event) => setSearch(event.target.value)}
          onKeyDown={(event) => {
            if (event.key === "Enter") onSearch();
          }}
        />
      </label>
      <div className="topbar-actions">
        <button className="icon-button notification" aria-label="Notifications" onClick={onSearch}>
          <Bell size={18} />
          <span />
        </button>
        <button className="profile-trigger" onClick={onSignOut} title="Sign out">
          <Avatar name={name} />
          <span>
            <b>{name}</b>
            <small>{role}</small>
          </span>
          <LogOut size={14} />
        </button>
      </div>
    </header>
  );
}

function PortalHeading({
  title,
  subtitle,
  icon: Icon,
  action,
}: {
  title: string;
  subtitle: string;
  icon: IconType;
  action?: ReactNode;
}) {
  return (
    <section className="portal-heading">
      <div>
        <span>
          <Icon size={26} />
        </span>
        <div>
          <h1>{title}</h1>
          <p>{subtitle}</p>
        </div>
      </div>
      {action}
    </section>
  );
}

function LifecycleTimeline({ stage }: { stage: number }) {
  return (
    <div className="repair-timeline">
      {repairStages.map((label, index) => (
        <div className={index < stage ? "done" : index === stage ? "current" : ""} key={label}>
          <span>{index < stage ? <Check size={13} /> : index + 1}</span>
          <div>
            <b>{label}</b>
            <small>
              {index < stage ? "Completed" : index === stage ? "Current status" : "Upcoming"}
            </small>
          </div>
        </div>
      ))}
    </div>
  );
}

function CustomerHome({ stage, onPage }: { stage: number; onPage: (page: CustomerPage) => void }) {
  const waitingApproval = stage === 3;
  return (
    <>
      <PortalHeading
        title="Welcome back, Ara!"
        subtitle="Here’s an overview of your computer repair needs."
        icon={Gauge}
        action={
          <button className="button button--primary" onClick={() => onPage("Appointments")}>
            <Plus size={16} /> Book appointment
          </button>
        }
      />
      <div className="summary-grid customer-summary">
        <SummaryCard
          icon={CalendarDays}
          label="Next Appointment"
          value="Oct 17"
          trend={stage === 0 ? "Awaiting confirmation" : "10:00 AM · Confirmed"}
        />
        <SummaryCard
          icon={Wrench}
          label="Active Repair"
          value="R-1024"
          trend={repairStages[stage]}
          tone="green"
        />
        <SummaryCard
          icon={CircleDollarSign}
          label="Current Estimate"
          value="₱4,850"
          trend={waitingApproval ? "Your decision is needed" : "Includes parts & labor"}
          tone="orange"
        />
        <SummaryCard
          icon={Computer}
          label="Registered Devices"
          value="2"
          trend="ACER laptop selected"
          tone="purple"
        />
      </div>
      {waitingApproval && (
        <section className="decision-banner">
          <span>
            <ShieldCheck size={24} />
          </span>
          <div>
            <small>ACTION REQUIRED</small>
            <h3>Your repair estimate is ready</h3>
            <p>Review Vina’s diagnosis and approve or decline the ₱4,850.00 estimate.</p>
          </div>
          <button className="button button--primary" onClick={() => onPage("My Repairs")}>
            Review estimate
          </button>
        </section>
      )}
      {stage === 6 && (
        <section className="decision-banner decision-banner--success">
          <PackageCheck size={28} />
          <div>
            <small>READY FOR PICKUP</small>
            <h3>Your ACER laptop is ready</h3>
            <p>Payment received. Bring a valid ID and receipt OR-0184 to the CRSM front desk.</p>
          </div>
          <button className="button button--primary" onClick={() => onPage("Payments & Receipts")}>
            View receipt
          </button>
        </section>
      )}
      <div className="portal-grid">
        <section className="panel">
          <div className="panel-title">
            <div>
              <h3>Repair status</h3>
              <p>ACER Predator Helios Neo 16 · R-1024</p>
            </div>
            <button className="text-button" onClick={() => onPage("My Repairs")}>
              View details <ChevronRight size={14} />
            </button>
          </div>
          <div className="repair-glance">
            <Status value={stage === 6 ? "Ready for Pickup" : repairStages[stage]} />
            <strong>{Math.round(((stage + 1) / repairStages.length) * 100)}%</strong>
            <i>
              <em style={{ width: `${((stage + 1) / repairStages.length) * 100}%` }} />
            </i>
            <p>
              Next action:{" "}
              <b>
                {stage === 3
                  ? "Approve or decline estimate"
                  : stage === 6
                    ? "Collect your device"
                    : "We’ll notify you when action is needed"}
              </b>
            </p>
          </div>
        </section>
        <section className="panel">
          <div className="panel-title">
            <div>
              <h3>My computers</h3>
              <p>Devices registered to your account</p>
            </div>
            <button className="text-button" onClick={() => onPage("My Computers")}>
              Manage <ChevronRight size={14} />
            </button>
          </div>
          <div className="device-tiles">
            <div>
              <Laptop size={28} />
              <span>
                <b>ACER Predator Helios</b>
                <small>ACERPH00123 · In service</small>
              </span>
            </div>
            <div>
              <Computer size={28} />
              <span>
                <b>ACER Aspire TC</b>
                <small>ACTC55198 · Active</small>
              </span>
            </div>
          </div>
        </section>
      </div>
    </>
  );
}

function CustomerComputers({
  data,
  onSave,
  onBook,
}: {
  data: DemoData;
  onSave: (row: Row) => void;
  onBook: () => void;
}) {
  const [editing, setEditing] = useState<Row | null>(null);
  const [error, setError] = useState("");
  const customerDevices = data.computers.filter((item) => item.customer === "Ara Bernabe");
  const newDevice = (): Row => ({
    id: `D-${String(data.computers.length + 1).padStart(3, "0")}`,
    customer: "Ara Bernabe",
    type: "Laptop",
    brand: "",
    model: "",
    serial: "",
    status: "Active",
    added: "Oct 16, 2026",
  });
  const save = () => {
    if (!editing) return;
    if (
      !String(editing.brand).trim() ||
      !String(editing.model).trim() ||
      !String(editing.serial).trim()
    ) {
      setError("Brand, model, and serial number are required.");
      return;
    }
    onSave(editing);
    setEditing(null);
    setError("");
  };
  return (
    <>
      <PortalHeading
        title="My Computers"
        subtitle="Register and manage the devices you bring to CRSM."
        icon={Computer}
        action={
          <button className="button button--primary" onClick={() => setEditing(newDevice())}>
            <Plus size={16} /> Register computer
          </button>
        }
      />
      {editing && (
        <section className="panel inline-form">
          <ContentTitle
            icon={Plus}
            title={
              customerDevices.some((item) => item.id === editing.id)
                ? "Edit computer"
                : "Register a computer"
            }
            subtitle="Device records are visible only to you and authorized CRSM staff."
          />
          <div className="form-grid">
            {[
              ["type", "Device type"],
              ["brand", "Brand"],
              ["model", "Model"],
              ["serial", "Serial number"],
            ].map(([key, label]) => (
              <label className="field" key={key}>
                <span>{label}</span>
                <input
                  value={String(editing[key] || "")}
                  onChange={(event) =>
                    setEditing((current) =>
                      current ? { ...current, [key]: event.target.value } : current,
                    )
                  }
                />
              </label>
            ))}
          </div>
          {error && <div className="form-error">{error}</div>}
          <div className="save-buttons">
            <button className="button button--ghost" onClick={() => setEditing(null)}>
              Cancel
            </button>
            <button className="button button--primary" onClick={save}>
              Save computer
            </button>
          </div>
        </section>
      )}
      <section className="panel device-list">
        {customerDevices.length ? (
          customerDevices.map((device, index) => (
            <article key={String(device.id)}>
              <span className="device-image">
                {index ? <Computer size={36} /> : <Laptop size={36} />}
              </span>
              <div>
                <small>{String(device.type).toUpperCase()}</small>
                <h3>
                  {String(device.brand)} {String(device.model)}
                </h3>
                <p>Serial number: {String(device.serial)}</p>
              </div>
              <Status value={String(device.status)} />
              <button className="button button--ghost" onClick={() => setEditing({ ...device })}>
                Edit
              </button>
              <button className="button button--ghost" onClick={onBook}>
                Book service
              </button>
            </article>
          ))
        ) : (
          <div className="empty-state">
            <Computer size={28} />
            <b>No computers registered</b>
            <span>Register a computer to book a service.</span>
          </div>
        )}
      </section>
    </>
  );
}

function CustomerAppointments({
  data,
  onBook,
  onCancel,
}: {
  data: DemoData;
  onBook: (details: {
    date: string;
    time: string;
    issue: string;
    photos: string[];
    deviceId: string;
  }) => void;
  onCancel: () => void;
}) {
  const [day, setDay] = useState(17);
  const [time, setTime] = useState("10:00 AM");
  const [issue, setIssue] = useState(data.repair.issue);
  const [photos, setPhotos] = useState<string[]>(data.repair.photos);
  const [selectedDevice, setSelectedDevice] = useState(data.repair.deviceId);
  const [error, setError] = useState("");
  const [confirmCancel, setConfirmCancel] = useState(false);
  const appointment = data.appointments.find((item) => item.id === data.repair.appointmentId);
  const booked = appointment && appointment.status !== "Cancelled";
  const uploadPhotos = (files: FileList | null) => {
    if (!files) return;
    const valid = Array.from(files).filter(
      (file) => file.type.startsWith("image/") && file.size <= 5 * 1024 * 1024,
    );
    if (valid.length !== files.length) {
      setError("Only JPG or PNG images up to 5 MB can be attached.");
    }
    valid.slice(0, 3).forEach((file) => {
      const reader = new FileReader();
      reader.onload = () => setPhotos((current) => [...current, String(reader.result)].slice(0, 3));
      reader.readAsDataURL(file);
    });
  };
  return (
    <>
      <PortalHeading
        title="Appointments"
        subtitle="Book and manage your CRSM drop-off schedule."
        icon={CalendarDays}
      />
      <div className="appointment-portal-grid">
        <section className="panel calendar-card">
          <div className="panel-title">
            <div>
              <h3>Select a drop-off date</h3>
              <p>October 2026</p>
            </div>
          </div>
          <div className="calendar-grid">
            {["S", "M", "T", "W", "T", "F", "S"].map((day, i) => (
              <b key={`${day}-${i}`}>{day}</b>
            ))}
            {Array.from({ length: 31 }, (_, i) => i + 1).map((dateNumber) => (
              <button
                className={dateNumber === day ? "selected" : ""}
                key={dateNumber}
                onClick={() => setDay(dateNumber)}
              >
                {dateNumber}
              </button>
            ))}
          </div>
        </section>
        <section className="panel booking-form">
          <h3>Appointment details</h3>
          <div className="time-slots">
            {["9:00 AM", "10:00 AM", "11:00 AM", "1:00 PM", "2:00 PM", "3:00 PM"].map((slot) => (
              <button
                className={slot === time ? "active" : ""}
                key={slot}
                onClick={() => setTime(slot)}
              >
                {slot}
              </button>
            ))}
          </div>
          <label className="field">
            <span>Computer</span>
            <select
              value={selectedDevice}
              onChange={(event) => setSelectedDevice(event.target.value)}
            >
              {data.computers
                .filter((item) => item.customer === "Ara Bernabe")
                .map((item) => (
                  <option value={String(item.id)} key={String(item.id)}>
                    {String(item.brand)} {String(item.model)}
                  </option>
                ))}
            </select>
          </label>
          <label className="field">
            <span>Describe the problem</span>
            <textarea value={issue} onChange={(event) => setIssue(event.target.value)} />
          </label>
          <label className="upload-zone">
            <Upload size={22} />
            <b>Attach damage photos</b>
            <span>JPG or PNG, up to 5 MB each</span>
            <input
              type="file"
              accept="image/png,image/jpeg"
              multiple
              onChange={(event) => uploadPhotos(event.target.files)}
            />
          </label>
          {photos.length > 0 && (
            <div className="upload-previews">
              {photos.map((photo, index) => (
                <span key={`${photo.slice(0, 30)}-${index}`}>
                  <img src={photo} alt={`Damage preview ${index + 1}`} />
                  <button
                    onClick={() => setPhotos((current) => current.filter((_, i) => i !== index))}
                  >
                    <X size={13} />
                  </button>
                </span>
              ))}
            </div>
          )}
          {error && <div className="form-error">{error}</div>}
          <button
            className="button button--primary full-button"
            onClick={() => {
              if (issue.trim().length < 15) {
                setError("Describe the problem in at least 15 characters.");
                return;
              }
              onBook({
                date: `Oct ${day}, 2026`,
                time,
                issue,
                photos,
                deviceId: selectedDevice,
              });
              setError("");
            }}
          >
            <CalendarDays size={16} /> Book drop-off
          </button>
        </section>
      </div>
      {booked && (
        <section className="panel appointment-ticket">
          <span className="ticket-date">
            <b>{day}</b> OCT
          </span>
          <div>
            <h3>Hardware diagnostics drop-off</h3>
            <p>{time} · ACER Predator Helios Neo 16</p>
          </div>
          <Status value={String(appointment.status)} />
          <button
            className="button button--ghost"
            onClick={() =>
              onBook({
                date: `Oct ${day}, 2026`,
                time,
                issue,
                photos,
                deviceId: selectedDevice,
              })
            }
          >
            Reschedule
          </button>
          <button className="button cancel-button" onClick={() => setConfirmCancel(true)}>
            Cancel
          </button>
        </section>
      )}
      {confirmCancel && (
        <div className="modal-backdrop">
          <section className="confirm-dialog" role="alertdialog" aria-modal="true">
            <span className="confirm-icon">
              <CalendarDays size={24} />
            </span>
            <h3>Cancel this appointment?</h3>
            <p>The drop-off slot will be released. You can book another eligible date later.</p>
            <div className="save-buttons">
              <button className="button button--ghost" onClick={() => setConfirmCancel(false)}>
                Keep appointment
              </button>
              <button
                className="button cancel-button"
                onClick={() => {
                  onCancel();
                  setConfirmCancel(false);
                }}
              >
                Cancel appointment
              </button>
            </div>
          </section>
        </div>
      )}
    </>
  );
}

function CustomerRepair({
  data,
  onDecision,
}: {
  data: DemoData;
  onDecision: (decision: RepairDecision) => void;
}) {
  const stage = data.repair.stage;
  const canDecide = stage === 3;
  const [confirmDecline, setConfirmDecline] = useState(false);
  const total = data.repair.estimateParts + data.repair.estimateLabor;
  return (
    <>
      <PortalHeading
        title="Repair R-1024"
        subtitle="ACER Predator Helios Neo 16 · Submitted Oct 16, 2026"
        icon={Wrench}
        action={<Status value={stage === 6 ? "Ready for Pickup" : repairStages[stage]} />}
      />
      <div className="repair-detail-grid">
        <section className="panel">
          <div className="panel-title">
            <div>
              <h3>Diagnosis & estimate</h3>
              <p>Prepared by Vina Saji Fukiko</p>
            </div>
          </div>
          <div className="diagnosis-box">
            <b>Customer-facing diagnosis</b>
            <p>
              {data.repair.diagnosis ||
                "The technician has not submitted a customer-facing diagnosis yet."}
            </p>
          </div>
          <div className="estimate-lines">
            <span>
              Replacement parts <b>₱{data.repair.estimateParts.toLocaleString("en-PH")}.00</b>
            </span>
            <span>
              Labor and testing <b>₱{data.repair.estimateLabor.toLocaleString("en-PH")}.00</b>
            </span>
            <strong>
              Estimated total <b>₱{total.toLocaleString("en-PH")}.00</b>
            </strong>
          </div>
          {canDecide && (
            <div className="decision-actions">
              <button className="button button--primary" onClick={() => onDecision("approved")}>
                <Check size={16} /> Approve Repair
              </button>
              <button className="button decline-button" onClick={() => setConfirmDecline(true)}>
                Decline Repair
              </button>
            </div>
          )}
          {data.repair.decision === "approved" && (
            <div className="success-note">
              <Check size={18} /> Estimate approved by Ara Bernabe on Oct 16 at 2:18 PM.
            </div>
          )}
          {data.repair.decision === "declined" && (
            <div className="form-error">
              Estimate declined. CRSM will contact you about return options.
            </div>
          )}
        </section>
        <section className="panel">
          <div className="panel-title">
            <div>
              <h3>Repair timeline</h3>
              <p>Updates shared by CRSM</p>
            </div>
          </div>
          <LifecycleTimeline stage={stage} />
        </section>
      </div>
      <div className="portal-grid">
        <section className="panel repair-info">
          <h3>Reported problem</h3>
          <p>{data.repair.issue}</p>
          <div className="damage-photos">
            {data.repair.photos.length ? (
              data.repair.photos.map((photo, index) => (
                <span key={`${photo.slice(0, 20)}-${index}`}>
                  <img src={photo} alt={`Damage ${index + 1}`} />
                </span>
              ))
            ) : (
              <span>
                <Camera size={24} />
                No photos attached
              </span>
            )}
          </div>
        </section>
        <section className="panel pickup-card">
          <PackageCheck size={27} />
          <div>
            <h3>Pickup information</h3>
            <p>
              {stage >= 6
                ? "Ready at CRSM Batac City. Bring a valid ID and receipt OR-0184."
                : "Pickup details will appear after technical work and payment are complete."}
            </p>
          </div>
        </section>
      </div>
      {confirmDecline && (
        <div className="modal-backdrop">
          <section className="confirm-dialog" role="alertdialog" aria-modal="true">
            <span className="confirm-icon">
              <X size={24} />
            </span>
            <h3>Decline this estimate?</h3>
            <p>The technician cannot begin work. CRSM will prepare the device for return.</p>
            <div className="save-buttons">
              <button className="button button--ghost" onClick={() => setConfirmDecline(false)}>
                Go back
              </button>
              <button
                className="button cancel-button"
                onClick={() => {
                  onDecision("declined");
                  setConfirmDecline(false);
                }}
              >
                Decline estimate
              </button>
            </div>
          </section>
        </div>
      )}
    </>
  );
}

function CustomerServices({ services, onBook }: { services: Row[]; onBook: () => void }) {
  return (
    <>
      <PortalHeading
        title="Services & Pricing"
        subtitle="Quality computer service with clear, competitive pricing."
        icon={FileText}
      />
      <div className="service-cards">
        {services
          .filter((service) => service.status !== "Inactive")
          .slice(0, 6)
          .map((service) => (
            <article key={service.id}>
              <span>
                <Wrench size={24} />
              </span>
              <small>{service.category}</small>
              <h3>{service.service}</h3>
              <p>{String(service.description)}</p>
              <strong>{service.price}</strong>
              <button className="button button--primary" onClick={onBook}>
                Book this service <ChevronRight size={14} />
              </button>
            </article>
          ))}
      </div>
    </>
  );
}

function CustomerPayments({ data, notify }: { data: DemoData; notify: (message: string) => void }) {
  const paid = data.repair.paid;
  const total = data.repair.estimateParts + data.repair.estimateLabor;
  const totalLabel = `₱${total.toLocaleString("en-PH")}.00`;
  const downloadReceipt = () => {
    if (!paid) return;
    downloadText(
      `${data.repair.receipt}.txt`,
      `CRSM OFFICIAL RECEIPT\n${data.repair.receipt}\nCustomer: Ara Bernabe\nRepair: ${data.repair.id}\nDisplay panel replacement\nTotal paid: ${totalLabel}\nPayment method: GCash\n`,
    );
    notify("Receipt downloaded.");
  };
  return (
    <>
      <PortalHeading
        title="Payments & Receipts"
        subtitle="View charges, payment status, and your CRSM receipts."
        icon={CircleDollarSign}
      />
      <div className="summary-grid">
        <SummaryCard
          icon={FileText}
          label="Final Charge"
          value={totalLabel}
          trend={`Repair ${data.repair.id}`}
        />
        <SummaryCard
          icon={CircleDollarSign}
          label="Amount Paid"
          value={paid ? totalLabel : "₱0.00"}
          trend={paid ? "Paid via GCash" : "Payment not yet due"}
          tone="green"
        />
        <SummaryCard
          icon={Clock3}
          label="Outstanding Balance"
          value={paid ? "₱0.00" : totalLabel}
          trend={paid ? "Fully settled" : "Awaiting repair completion"}
          tone="orange"
        />
        <SummaryCard
          icon={FileText}
          label="Receipt"
          value={paid ? data.repair.receipt : "Pending"}
          trend={paid ? "Issued Oct 16, 2026" : "Issued after payment"}
          tone="purple"
        />
      </div>
      <section className="panel receipt-card">
        <div>
          <Logo compact />
          <span>
            <small>OFFICIAL RECEIPT</small>
            <b>{paid ? data.repair.receipt : "Not yet issued"}</b>
          </span>
        </div>
        <dl>
          <div>
            <dt>Customer</dt>
            <dd>Ara Bernabe</dd>
          </div>
          <div>
            <dt>Repair</dt>
            <dd>R-1024 · Display panel replacement</dd>
          </div>
          <div>
            <dt>Payment method</dt>
            <dd>{paid ? "GCash · GC-161026-184" : "—"}</dd>
          </div>
          <div>
            <dt>Total</dt>
            <dd>{totalLabel}</dd>
          </div>
        </dl>
        <button className="button button--primary" disabled={!paid} onClick={downloadReceipt}>
          <Download size={16} /> Download receipt
        </button>
      </section>
    </>
  );
}

function CustomerProfile({
  data,
  onSave,
}: {
  data: DemoData;
  onSave: (profile: DemoData["customerProfile"]) => void;
}) {
  const [profile, setProfile] = useState(data.customerProfile);
  const [error, setError] = useState("");
  const save = () => {
    if (!profile.name.trim() || !profile.email.includes("@") || profile.phone.length < 8) {
      setError("Enter a valid name, email address, and phone number.");
      return;
    }
    onSave(profile);
    setError("");
  };
  return (
    <>
      <PortalHeading
        title="Profile"
        subtitle="Manage your personal details and account preferences."
        icon={User}
      />
      <div className="portal-grid">
        <section className="panel profile-panel">
          <ContentTitle
            icon={User}
            title="Personal information"
            subtitle="Used for appointments, repair updates, and receipts."
          />
          <div className="form-grid">
            {[
              ["name", "Full name"],
              ["email", "Email address"],
              ["phone", "Phone number"],
              ["address", "Address"],
            ].map(([key, label]) => (
              <label className="field" key={key}>
                <span>{label}</span>
                <input
                  value={profile[key as keyof typeof profile]}
                  onChange={(event) =>
                    setProfile((current) => ({ ...current, [key]: event.target.value }))
                  }
                />
              </label>
            ))}
          </div>
          {error && <div className="form-error">{error}</div>}
          <div className="save-buttons">
            <button
              className="button button--ghost"
              onClick={() => setProfile(data.customerProfile)}
            >
              Discard
            </button>
            <button className="button button--primary" onClick={save}>
              Save changes
            </button>
          </div>
        </section>
        <section className="panel profile-panel">
          <ContentTitle
            icon={Bell}
            title="Notification preferences"
            subtitle="Choose how CRSM keeps you updated."
          />
          {[
            "Appointment reminders",
            "Estimate approvals",
            "Repair status updates",
            "Pickup notices",
          ].map((label) => (
            <SettingLine label={label} key={label}>
              <Toggle />
            </SettingLine>
          ))}
        </section>
      </div>
    </>
  );
}

function CustomerPortal({
  page,
  data,
  onPage,
  update,
  notify,
}: {
  page: CustomerPage;
  data: DemoData;
  onPage: (page: CustomerPage) => void;
  update: (updater: (current: DemoData) => DemoData) => void;
  notify: (message: string) => void;
}) {
  const stage = data.repair.stage;
  if (page === "Home") return <CustomerHome stage={stage} onPage={onPage} />;
  if (page === "My Computers")
    return (
      <CustomerComputers
        data={data}
        onBook={() => onPage("Appointments")}
        onSave={(row) => {
          update((current) => ({
            ...current,
            computers: current.computers.some((item) => item.id === row.id)
              ? current.computers.map((item) => (item.id === row.id ? row : item))
              : [row, ...current.computers],
          }));
          notify("Computer saved and available for booking.");
        }}
      />
    );
  if (page === "Appointments")
    return (
      <CustomerAppointments
        data={data}
        onBook={({ date, time, issue, photos, deviceId }) => {
          const selected = data.computers.find((item) => item.id === deviceId);
          update((current) => ({
            ...current,
            appointments: current.appointments.map((item) =>
              item.id === current.repair.appointmentId
                ? {
                    ...item,
                    date: `${date} · ${time}`,
                    status: "Pending",
                    notes: issue,
                    device: `${String(selected?.type || "Computer")} (${String(selected?.brand || "")})`,
                  }
                : item,
            ),
            repair: {
              ...current.repair,
              stage: 0,
              issue,
              photos,
              deviceId,
              decision: "pending",
            },
          }));
          notify("Drop-off appointment saved and sent to Admin for confirmation.");
        }}
        onCancel={() => {
          update((current) => ({
            ...current,
            appointments: current.appointments.map((item) =>
              item.id === current.repair.appointmentId ? { ...item, status: "Cancelled" } : item,
            ),
          }));
          notify("Appointment cancelled.");
        }}
      />
    );
  if (page === "My Repairs")
    return (
      <CustomerRepair
        data={data}
        onDecision={(decision) => {
          update((current) => ({
            ...current,
            repair: {
              ...current.repair,
              decision,
              stage: decision === "approved" ? 4 : 3,
            },
          }));
          notify(
            decision === "approved"
              ? "Estimate approved. The technician can begin work."
              : "Estimate declined. The technician has been notified.",
          );
        }}
      />
    );
  if (page === "Services & Pricing")
    return <CustomerServices services={data.services} onBook={() => onPage("Appointments")} />;
  if (page === "Payments & Receipts") return <CustomerPayments data={data} notify={notify} />;
  return (
    <CustomerProfile
      data={data}
      onSave={(profile) => {
        update((current) => ({
          ...current,
          customerProfile: profile,
          customers: current.customers.map((item) =>
            item.id === current.repair.customerId
              ? {
                  ...item,
                  name: profile.name,
                  email: profile.email,
                  phone: profile.phone,
                  address: profile.address,
                }
              : item,
          ),
        }));
        notify("Profile saved.");
      }}
    />
  );
}

function TechnicianQueue({
  data,
  onSubmitDiagnosis,
  onComplete,
}: {
  data: DemoData;
  onSubmitDiagnosis: (details: {
    tests: string;
    diagnosis: string;
    parts: number;
    labor: number;
  }) => void;
  onComplete: (notes: string) => void;
}) {
  const stage = data.repair.stage;
  const [detail, setDetail] = useState(true);
  const [tests, setTests] = useState(data.repair.tests);
  const [diagnosis, setDiagnosis] = useState(data.repair.diagnosis);
  const [parts, setParts] = useState(String(data.repair.estimateParts));
  const [labor, setLabor] = useState(String(data.repair.estimateLabor));
  const [workNotes, setWorkNotes] = useState(data.repair.workNotes);
  const [error, setError] = useState("");
  const assigned = data.repair.technician === "Vina Saji Fukiko";
  return (
    <>
      <PortalHeading
        title="My Assigned Repairs"
        subtitle="Prioritized work assigned to Vina Saji Fukiko."
        icon={Wrench}
      />
      <div className="queue-groups">
        {[
          ["Awaiting diagnosis", stage === 2 ? 1 : 0, "Diagnose device and prepare an estimate"],
          ["Awaiting customer approval", stage === 3 ? 1 : 0, "Estimate sent to Ara Bernabe"],
          ["Approved for repair", stage === 4 ? 1 : 0, "Complete approved technical work"],
        ].map(([label, count, copy], index) => (
          <button
            className={Number(count) ? "has-work" : ""}
            key={String(label)}
            onClick={() => setDetail(true)}
          >
            <span className={`queue-dot queue-dot--${index}`} />
            <span>
              <b>{label}</b>
              <small>{copy}</small>
            </span>
            <strong>{count}</strong>
          </button>
        ))}
      </div>
      {!assigned && (
        <section className="panel empty-state">
          <Wrench size={28} />
          <b>No matching assigned repair</b>
          <span>R-1024 will appear after Admin checks in and assigns the device to you.</span>
        </section>
      )}
      {detail && assigned && (
        <div className="technician-detail">
          <section className="panel job-main">
            <div className="job-header">
              <div>
                <small>REPAIR R-1024 · PRIORITY NORMAL</small>
                <h2>ACER Predator Helios Neo 16</h2>
                <p>Ara Bernabe · Checked in Oct 17 at 10:12 AM</p>
              </div>
              <Status
                value={
                  stage === 2
                    ? "Diagnosing"
                    : stage === 3
                      ? "Pending"
                      : stage >= 5
                        ? "Completed"
                        : "In Progress"
                }
              />
            </div>
            <div className="job-facts">
              <div>
                <small>Reported problem</small>
                <p>{data.repair.issue}</p>
              </div>
              <div>
                <small>Check-in condition</small>
                <p>{data.repair.condition}</p>
              </div>
              <div>
                <small>Accessories received</small>
                <p>{data.repair.accessories}</p>
              </div>
              <div>
                <small>Appointment</small>
                <p>Oct 17, 2026 · 10:00 AM · Hardware Diagnostics</p>
              </div>
            </div>
            <div className="damage-photos technician-photos">
              {data.repair.photos.length ? (
                data.repair.photos.map((photo, index) => (
                  <span key={`${photo.slice(0, 20)}-${index}`}>
                    <img src={photo} alt={`Customer damage ${index + 1}`} />
                  </span>
                ))
              ) : (
                <span>
                  <Camera size={25} /> No customer photos
                </span>
              )}
            </div>
            {stage === 2 && (
              <div className="technician-form">
                <label className="field">
                  <span>Tests performed</span>
                  <textarea value={tests} onChange={(event) => setTests(event.target.value)} />
                </label>
                <label className="field">
                  <span>Technical diagnosis</span>
                  <textarea
                    value={diagnosis}
                    onChange={(event) => setDiagnosis(event.target.value)}
                  />
                </label>
                <div className="form-grid">
                  <label className="field">
                    <span>Replacement parts (₱)</span>
                    <input
                      type="number"
                      min="0"
                      value={parts}
                      onChange={(e) => setParts(e.target.value)}
                    />
                  </label>
                  <label className="field">
                    <span>Labor & testing (₱)</span>
                    <input
                      type="number"
                      min="0"
                      value={labor}
                      onChange={(e) => setLabor(e.target.value)}
                    />
                  </label>
                </div>
                {error && <div className="form-error">{error}</div>}
                <button
                  className="button button--primary"
                  onClick={() => {
                    if (tests.trim().length < 10 || diagnosis.trim().length < 10) {
                      setError("Record the tests and diagnosis before submitting an estimate.");
                      return;
                    }
                    onSubmitDiagnosis({
                      tests,
                      diagnosis,
                      parts: Number(parts),
                      labor: Number(labor),
                    });
                    setError("");
                  }}
                >
                  Submit estimate for approval <ChevronRight size={16} />
                </button>
              </div>
            )}
            {stage === 3 && (
              <div className="waiting-state">
                {data.repair.decision === "declined" ? <X size={24} /> : <Clock3 size={24} />}
                <div>
                  <h3>
                    {data.repair.decision === "declined"
                      ? "Customer declined the estimate"
                      : "Waiting for customer decision"}
                  </h3>
                  <p>
                    {data.repair.decision === "declined"
                      ? "Do not begin work. Admin will coordinate device return."
                      : `The ₱${(data.repair.estimateParts + data.repair.estimateLabor).toLocaleString("en-PH")}.00 estimate was sent to Ara Bernabe.`}
                  </p>
                </div>
              </div>
            )}
            {stage === 4 && (
              <div className="technician-form">
                <div className="success-note">
                  <Check size={18} /> Customer approved ₱4,850.00 estimate at 2:18 PM.
                </div>
                <label className="field">
                  <span>Work performed</span>
                  <textarea
                    value={workNotes}
                    onChange={(event) => setWorkNotes(event.target.value)}
                  />
                </label>
                <button
                  className="button button--primary"
                  onClick={() => {
                    if (workNotes.trim().length < 15) {
                      setError("Describe the completed technical work before closing the job.");
                      return;
                    }
                    onComplete(workNotes);
                  }}
                >
                  <Check size={16} /> Mark technical work complete
                </button>
              </div>
            )}
            {stage >= 5 && (
              <div className="waiting-state waiting-state--done">
                <Check size={24} />
                <div>
                  <h3>Technical work completed</h3>
                  <p>Final testing passed. The repair is now with Admin for payment and release.</p>
                </div>
              </div>
            )}
          </section>
          <aside className="panel job-aside">
            <h3>Repair progress</h3>
            <LifecycleTimeline stage={stage} />
          </aside>
        </div>
      )}
    </>
  );
}

function TechnicianSchedule({ onOpen }: { onOpen: () => void }) {
  return (
    <>
      <PortalHeading
        title="My Schedule"
        subtitle="Your assigned appointments and repair deadlines."
        icon={CalendarDays}
      />
      <section className="panel schedule-list">
        {[
          ["09:00 AM", "R-1022 · HP Pavilion", "Final testing", "Ready for Pickup"],
          ["10:00 AM", "R-1024 · ACER Predator Helios", "Hardware diagnostics", "Confirmed"],
          ["01:30 PM", "R-1028 · Lenovo ThinkPad", "Battery replacement", "Pending"],
          ["03:00 PM", "R-1031 · Dell OptiPlex", "No boot diagnosis", "Confirmed"],
        ].map(([time, device, task, status]) => (
          <div key={time}>
            <span className="schedule-time">{time}</span>
            <div>
              <b>{device}</b>
              <small>{task}</small>
            </div>
            <Status value={status} />
            <button className="text-button" onClick={onOpen}>
              Open job <ChevronRight size={14} />
            </button>
          </div>
        ))}
      </section>
    </>
  );
}

function TechnicianProfile() {
  return (
    <>
      <PortalHeading
        title="Technician Profile"
        subtitle="Your CRSM staff information and service specialties."
        icon={UserCog}
      />
      <div className="portal-grid">
        <section className="panel profile-panel">
          <ContentTitle
            icon={User}
            title="Staff information"
            subtitle="Account details managed by an administrator."
          />
          <div className="form-grid">
            <Field label="Full name" value="Vina Saji Fukiko" />
            <Field label="Technician ID" value="N-002" />
            <Field label="Email address" value="vina.fukiko@email.com" />
            <Field label="Contact number" value="0918 234 5678" />
          </div>
        </section>
        <section className="panel profile-panel">
          <ContentTitle
            icon={Wrench}
            title="Skills & workload"
            subtitle="Assignment details visible to Admin."
          />
          <div className="skill-tags">
            <span>Hardware Repair</span>
            <span>Diagnostics</span>
            <span>Display Replacement</span>
          </div>
          <SettingLine label="Current assignments">
            <b>3</b>
          </SettingLine>
          <SettingLine label="Availability">
            <Status value="Active" />
          </SettingLine>
        </section>
      </div>
    </>
  );
}

function TechnicianPortal({
  page,
  data,
  onPage,
  update,
  notify,
}: {
  page: TechnicianPage;
  data: DemoData;
  onPage: (page: TechnicianPage) => void;
  update: (updater: (current: DemoData) => DemoData) => void;
  notify: (message: string) => void;
}) {
  if (page === "My Schedule")
    return <TechnicianSchedule onOpen={() => onPage("My Assigned Repairs")} />;
  if (page === "Profile") return <TechnicianProfile />;
  return (
    <TechnicianQueue
      data={data}
      onSubmitDiagnosis={({ tests, diagnosis, parts, labor }) => {
        update((current) => ({
          ...current,
          repair: {
            ...current.repair,
            stage: 3,
            tests,
            diagnosis,
            estimateParts: parts,
            estimateLabor: labor,
            decision: "pending",
          },
          repairs: current.repairs.map((item) =>
            item.id === current.repair.id
              ? {
                  ...item,
                  status: "Awaiting Approval",
                  cost: `₱${(parts + labor).toLocaleString("en-PH")}.00`,
                }
              : item,
          ),
        }));
        notify("Diagnosis and itemized estimate sent to the customer.");
      }}
      onComplete={(workNotes) => {
        update((current) => ({
          ...current,
          repair: { ...current.repair, stage: 5, workNotes },
          repairs: current.repairs.map((item) =>
            item.id === current.repair.id ? { ...item, status: "Completed" } : item,
          ),
        }));
        notify("Technical work marked complete. Admin has been notified.");
      }}
    />
  );
}

function RoleDock({
  role,
  onRole,
  onReset,
}: {
  role: Role;
  onRole: (role: Role) => void;
  onReset: () => void;
}) {
  const [confirmReset, setConfirmReset] = useState(false);
  return (
    <>
      <div className="role-dock">
        <span>Demo account</span>
        {(["Admin", "Customer", "Technician"] as Role[]).map((item) => (
          <button className={role === item ? "active" : ""} onClick={() => onRole(item)} key={item}>
            {item}
          </button>
        ))}
        <button className="reset-demo" onClick={() => setConfirmReset(true)}>
          Reset Demo Data
        </button>
      </div>
      {confirmReset && (
        <div className="modal-backdrop">
          <section className="confirm-dialog" role="alertdialog" aria-modal="true">
            <span className="confirm-icon">
              <Activity size={24} />
            </span>
            <h3>Reset all demo data?</h3>
            <p>
              This returns the shared workflow, records, uploads, and receipts to the beginning.
            </p>
            <div className="save-buttons">
              <button className="button button--ghost" onClick={() => setConfirmReset(false)}>
                Keep data
              </button>
              <button
                className="button cancel-button"
                onClick={() => {
                  onReset();
                  setConfirmReset(false);
                }}
              >
                Reset Demo Data
              </button>
            </div>
          </section>
        </div>
      )}
    </>
  );
}

function Auth({
  mode,
  onMode,
  onLogin,
}: {
  mode: "signin" | "signup";
  onMode: (m: "signin" | "signup") => void;
  onLogin: (role: Role) => void;
}) {
  const [show, setShow] = useState(false);
  const [submitted, setSubmitted] = useState(false);
  const [selectedRole, setSelectedRole] = useState<Role>("Customer");
  const [authMessage, setAuthMessage] = useState("");
  const submit = (e: FormEvent) => {
    e.preventDefault();
    setSubmitted(true);
    setTimeout(() => onLogin(mode === "signup" ? "Customer" : selectedRole), 450);
  };
  return (
    <main className="auth-page">
      <section className="auth-visual">
        <div className="auth-overlay" />
        <div className="auth-brand">
          <Logo />
          <span>Reliable repairs. Organized service.</span>
        </div>
        <div className="auth-quote">
          <Wrench size={28} />
          <h1>
            Your repair center,
            <br />
            running at its best.
          </h1>
          <p>
            Manage customers, appointments, technicians, repairs, and payments from one secure
            workspace.
          </p>
        </div>
        <p className="photo-credit">Photo by Samsung Memory on Unsplash</p>
      </section>
      <section className="auth-panel">
        <div className="auth-form-wrap">
          <div className="auth-mobile-logo">
            <Logo />
          </div>
          <small className="eyebrow">
            {mode === "signup" ? "CRSM CUSTOMER REGISTRATION" : "CRSM SECURE ACCESS"}
          </small>
          <h2>{mode === "signin" ? "Welcome back" : "Create your account"}</h2>
          <p>
            {mode === "signin"
              ? "Sign in to continue to your repair workspace."
              : "Create a customer account to book and track repairs."}
          </p>
          <form onSubmit={submit}>
            {mode === "signin" && (
              <>
                <div className="role-selector">
                  <span>Choose a demo account</span>
                  <div>
                    {(["Customer", "Technician", "Admin"] as Role[]).map((role) => (
                      <button
                        type="button"
                        className={selectedRole === role ? "active" : ""}
                        onClick={() => setSelectedRole(role)}
                        key={role}
                      >
                        {role}
                      </button>
                    ))}
                  </div>
                </div>
                <div className="demo-credentials">
                  <b>
                    {selectedRole === "Admin"
                      ? "Pablo Job"
                      : selectedRole === "Technician"
                        ? "Vina Saji Fukiko"
                        : "Ara Bernabe"}
                  </b>
                  <span>
                    {selectedRole === "Admin"
                      ? "pablo.job@crsm.com"
                      : selectedRole === "Technician"
                        ? "vina.fukiko@crsm.com"
                        : "ara.bernabe@email.com"}{" "}
                    · password: repaircenter
                  </span>
                </div>
              </>
            )}
            {mode === "signup" && <Field label="Full name" value="" />}
            <label className={`auth-field ${submitted ? "valid" : ""}`}>
              <span>Email address</span>
              <div>
                <User size={17} />
                <input
                  required
                  type="email"
                  value={
                    mode === "signin"
                      ? selectedRole === "Admin"
                        ? "pablo.job@crsm.com"
                        : selectedRole === "Technician"
                          ? "vina.fukiko@crsm.com"
                          : "ara.bernabe@email.com"
                      : undefined
                  }
                  onChange={() => undefined}
                  placeholder="name@company.com"
                />
                {submitted && <Check size={16} />}
              </div>
            </label>
            <label className="auth-field">
              <span>Password</span>
              <div>
                <KeyRound size={17} />
                <input
                  required
                  minLength={8}
                  type={show ? "text" : "password"}
                  defaultValue={mode === "signin" ? "repaircenter" : ""}
                  placeholder="At least 8 characters"
                />
                <button type="button" onClick={() => setShow(!show)}>
                  {show ? <EyeOff size={17} /> : <Eye size={17} />}
                </button>
              </div>
              {mode === "signup" && <small>Use 8+ characters with a number and symbol.</small>}
            </label>
            {mode === "signup" && (
              <PasswordField label="Confirm password" placeholder="Re-enter your password" />
            )}
            {mode === "signin" && (
              <div className="auth-options">
                <label>
                  <input type="checkbox" defaultChecked />
                  Remember me
                </label>
                <button
                  type="button"
                  onClick={() =>
                    setAuthMessage("Demo password reset instructions were sent to this account.")
                  }
                >
                  Forgot password?
                </button>
              </div>
            )}
            {authMessage && <div className="success-note">{authMessage}</div>}
            <button className="auth-submit" type="submit">
              {submitted ? (
                <>
                  <Check size={18} />
                  Verified
                </>
              ) : mode === "signin" ? (
                "Sign in to CRSM"
              ) : (
                "Create account"
              )}
              <ChevronRight size={18} />
            </button>
          </form>
          <div className="auth-switch">
            {mode === "signin" ? "New to CRSM?" : "Already have an account?"}{" "}
            <button onClick={() => onMode(mode === "signin" ? "signup" : "signin")}>
              {mode === "signin" ? "Create account" : "Sign in"}
            </button>
          </div>
          <div className="auth-secure">
            <ShieldCheck size={15} />
            Protected by CRSM secure access
          </div>
        </div>
      </section>
    </main>
  );
}

export default function App() {
  const { data, update, reset } = useDemoData();
  const [authenticated, setAuthenticated] = useState(false);
  const [authMode, setAuthMode] = useState<"signin" | "signup">("signin");
  const [role, setRole] = useState<Role>("Customer");
  const [page, setPage] = useState<Page>("Dashboard");
  const [customerPage, setCustomerPage] = useState<CustomerPage>("Home");
  const [technicianPage, setTechnicianPage] = useState<TechnicianPage>("My Assigned Repairs");
  const [mobileNav, setMobileNav] = useState(false);
  const [workflow, setWorkflow] = useState(false);
  const [toast, setToast] = useState("");
  const notify = (message: string) => {
    setToast(message);
    window.setTimeout(() => setToast(""), 4200);
  };
  const handleAdminAdvance = (patch?: Partial<DemoData["repair"]>) => {
    update((current) => {
      const stage = current.repair.stage;
      if (stage === 0) {
        return {
          ...current,
          appointments: current.appointments.map((item) =>
            item.id === current.repair.appointmentId ? { ...item, status: "Confirmed" } : item,
          ),
          repair: { ...current.repair, stage: 1 },
        };
      }
      if (stage === 1) {
        const nextRepair = { ...current.repair, ...patch, stage: 2 };
        return {
          ...current,
          repair: nextRepair,
          computers: current.computers.map((item) =>
            item.id === current.repair.deviceId ? { ...item, status: "Under Diagnosis" } : item,
          ),
          repairs: current.repairs.map((item) =>
            item.id === current.repair.id
              ? { ...item, technician: nextRepair.technician, status: "Diagnosing" }
              : item,
          ),
        };
      }
      if (stage === 5) {
        const total = current.repair.estimateParts + current.repair.estimateLabor;
        const receipt = "OR-0184";
        const payment: Row = {
          id: "P-2026-184",
          date: "Oct 16, 2026",
          customer: "Ara Bernabe",
          repair: current.repair.id,
          method: "GCash",
          amount: `₱${total.toLocaleString("en-PH")}.00`,
          status: "Paid",
          receipt,
        };
        return {
          ...current,
          repair: { ...current.repair, stage: 6, paid: true, receipt },
          payments: [payment, ...current.payments.filter((item) => item.id !== payment.id)],
          repairs: current.repairs.map((item) =>
            item.id === current.repair.id ? { ...item, status: "Ready for Pickup" } : item,
          ),
          computers: current.computers.map((item) =>
            item.id === current.repair.deviceId ? { ...item, status: "Ready for Pickup" } : item,
          ),
        };
      }
      if (stage === 6) {
        return {
          ...current,
          repair: { ...current.repair, stage: 7, collected: true },
          computers: current.computers.map((item) =>
            item.id === current.repair.deviceId ? { ...item, status: "Active" } : item,
          ),
          repairs: current.repairs.map((item) =>
            item.id === current.repair.id ? { ...item, status: "Completed" } : item,
          ),
        };
      }
      return current;
    });
    const labels: Record<number, string> = {
      0: "Appointment confirmed.",
      1: "Device checked in and technician assigned.",
      5: "Payment recorded, receipt issued, and device marked ready for pickup.",
      6: "Collection logged. Workflow complete.",
    };
    notify(labels[data.repair.stage] || "Shared repair record updated.");
  };
  const accountPages = useMemo(
    () => ["My Profile", "Account Settings", "Change Password", "Login Activity"].includes(page),
    [page],
  );
  if (!authenticated)
    return (
      <Auth
        mode={authMode}
        onMode={setAuthMode}
        onLogin={(signedInRole) => {
          if (authMode === "signup") {
            update((current) => ({
              ...current,
              users: current.users.some((item) => item.email === "new.customer@crsm.demo")
                ? current.users
                : [
                    ...current.users,
                    {
                      id: `U-${String(current.users.length + 1).padStart(3, "0")}`,
                      name: "New CRSM Customer",
                      username: "new.customer",
                      email: "new.customer@crsm.demo",
                      role: "Customer",
                      status: "Active",
                      created: "Oct 16, 2026",
                      login: "New account",
                    },
                  ],
            }));
          }
          setRole(signedInRole);
          setCustomerPage("Home");
          setTechnicianPage("My Assigned Repairs");
          setPage("Dashboard");
          setAuthenticated(true);
        }}
      />
    );
  const content =
    page === "Dashboard" ? (
      <Dashboard
        startWorkflow={() => setWorkflow(true)}
        repairStage={data.repair.stage}
        data={data}
        onAdvance={handleAdminAdvance}
        onOpen={setPage}
      />
    ) : listConfigs[page] ? (
      <ListPage page={page} data={data} update={update} notify={notify} />
    ) : page === "Reports" ? (
      <Reports data={data} notify={notify} />
    ) : page === "Settings" ? (
      <SettingsPage notify={notify} />
    ) : accountPages ? (
      <AccountPage page={page} onPage={setPage} notify={notify} />
    ) : null;
  if (role === "Customer") {
    return (
      <div className="app-shell portal-shell">
        <PortalSidebar
          role="Customer"
          page={customerPage}
          onPage={setCustomerPage}
          open={mobileNav}
          onClose={() => setMobileNav(false)}
          onLogout={() => setAuthenticated(false)}
        />
        <div className="app-main">
          <PortalTopbar
            role="Customer"
            onMenu={() => setMobileNav(true)}
            onSignOut={() => setAuthenticated(false)}
            onSearch={() => setCustomerPage("My Repairs")}
          />
          <main className="page-content portal-content">
            <CustomerPortal
              page={customerPage}
              data={data}
              onPage={setCustomerPage}
              update={update}
              notify={notify}
            />
          </main>
        </div>
        {mobileNav && (
          <button
            className="nav-backdrop"
            onClick={() => setMobileNav(false)}
            aria-label="Close navigation"
          />
        )}
        <RoleDock
          role={role}
          onRole={(nextRole) => {
            setRole(nextRole);
            setMobileNav(false);
          }}
          onReset={() => {
            reset();
            setCustomerPage("Home");
            notify("Demo data reset. The workflow is ready to replay.");
          }}
        />
        {toast && (
          <div className="toast">
            <Check size={18} />
            {toast}
            <button onClick={() => setToast("")}>
              <X size={16} />
            </button>
          </div>
        )}
      </div>
    );
  }
  if (role === "Technician") {
    return (
      <div className="app-shell portal-shell">
        <PortalSidebar
          role="Technician"
          page={technicianPage}
          onPage={setTechnicianPage}
          open={mobileNav}
          onClose={() => setMobileNav(false)}
          onLogout={() => setAuthenticated(false)}
        />
        <div className="app-main">
          <PortalTopbar
            role="Technician"
            onMenu={() => setMobileNav(true)}
            onSignOut={() => setAuthenticated(false)}
            onSearch={() => setTechnicianPage("My Assigned Repairs")}
          />
          <main className="page-content portal-content">
            <TechnicianPortal
              page={technicianPage}
              data={data}
              onPage={setTechnicianPage}
              update={update}
              notify={notify}
            />
          </main>
        </div>
        {mobileNav && (
          <button
            className="nav-backdrop"
            onClick={() => setMobileNav(false)}
            aria-label="Close navigation"
          />
        )}
        <RoleDock
          role={role}
          onRole={(nextRole) => {
            setRole(nextRole);
            setMobileNav(false);
          }}
          onReset={() => {
            reset();
            setTechnicianPage("My Assigned Repairs");
            notify("Demo data reset. The workflow is ready to replay.");
          }}
        />
        {toast && (
          <div className="toast">
            <Check size={18} />
            {toast}
            <button onClick={() => setToast("")}>
              <X size={16} />
            </button>
          </div>
        )}
      </div>
    );
  }
  return (
    <div className="app-shell">
      <Sidebar
        page={page}
        onPage={setPage}
        open={mobileNav}
        onClose={() => setMobileNav(false)}
        onLogout={() => setAuthenticated(false)}
      />
      <div className="app-main">
        <Topbar
          onMenu={() => setMobileNav(true)}
          onPage={setPage}
          onSignOut={() => setAuthenticated(false)}
        />
        <main className="page-content">{content}</main>
      </div>
      {mobileNav && (
        <button
          className="nav-backdrop"
          onClick={() => setMobileNav(false)}
          aria-label="Close navigation"
        />
      )}
      {workflow && (
        <WorkflowModal
          onClose={() => setWorkflow(false)}
          onDone={() => {
            setWorkflow(false);
            handleAdminAdvance();
            setPage("Payments");
          }}
        />
      )}
      {toast && (
        <div className="toast">
          <Check size={18} />
          {toast}
          <button onClick={() => setToast("")}>
            <X size={16} />
          </button>
        </div>
      )}
      <RoleDock
        role={role}
        onRole={(nextRole) => {
          setRole(nextRole);
          setMobileNav(false);
        }}
        onReset={() => {
          reset();
          setPage("Dashboard");
          notify("Demo data reset. The workflow is ready to replay.");
        }}
      />
    </div>
  );
}
