package com.knatware.aiworkforce.bootstrap;

import com.knatware.aiworkforce.model.*;
import com.knatware.aiworkforce.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Seeds an initial dataset on first run so the platform is immediately usable:
 *  - the default admin login (admin/admin, must change password at first login)
 *  - a sample AI-staffed company with departments, levels, and a few AI staff
 *
 * This makes the app demoable the moment it starts, with zero manual setup.
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private final AdminUserRepository admins;
    private final CompanyRepository companies;
    private final DepartmentRepository departments;
    private final LevelRepository levels;
    private final StaffRepository staff;
    private final PasswordEncoder encoder;

    public DataSeeder(AdminUserRepository admins, CompanyRepository companies,
                      DepartmentRepository departments, LevelRepository levels,
                      StaffRepository staff, PasswordEncoder encoder) {
        this.admins = admins; this.companies = companies; this.departments = departments;
        this.levels = levels; this.staff = staff; this.encoder = encoder;
    }

    @Override
    public void run(String... args) {
        if (admins.count() > 0) return; // already seeded

        // --- default admin: admin/admin, forced change at first login ---
        AdminUser admin = new AdminUser();
        admin.setUsername("admin");
        admin.setPasswordHash(encoder.encode("admin"));
        admin.setRole("ADMIN");
        admin.setMustChangePassword(true);
        admins.save(admin);

        // --- sample company ---
        Company company = new Company();
        company.setName("Knatware AI Company Ltd");
        company.setDescription("A fully AI-staffed customer operations company.");
        company.setUrl("https://knatware.com");
        company.setAddress("1 Innovation Way");
        company.setIndustry("Customer Support");
        companies.save(company);

        // --- departments & levels ---
        Department support = departments.save(new Department("Customer Support"));
        Department sales = departments.save(new Department("Sales"));
        Level associate = levels.save(new Level("Associate", 1));
        Level lead = levels.save(new Level("Team Lead", 3));

        // --- a manager AI ---
        Staff manager = new Staff();
        manager.setFullName("Ada (Support Lead)");
        manager.setEmail("ada@example.com");
        manager.setPosition("Customer Support Lead");
        manager.setFunction("Support Lead");
        manager.setDuties("Oversees support agents, handles escalations.");
        manager.setDepartment(support);
        manager.setLevel(lead);
        manager.setType(StaffType.AI);
        manager.setPhoneExtension("1001");
        manager.setSystemPrompt("You are Ada, a calm, expert customer support lead. Resolve escalations clearly.");
        manager.setModel("gpt-4o");
        manager.setConnector(ConnectorType.NONE); // simulated until a backend is attached
        manager.setVoiceEnabled(true);
        manager.setVoiceProvider("elevenlabs");
        staff.save(manager);

        // --- a support agent AI reporting to Ada ---
        Staff agent = new Staff();
        agent.setFullName("Max (Support Agent)");
        agent.setEmail("max@example.com");
        agent.setPosition("Customer Support Agent");
        agent.setFunction("Support Agent");
        agent.setDuties("Answers tier-1 customer questions.");
        agent.setDepartment(support);
        agent.setLevel(associate);
        agent.setReportsTo(manager);
        agent.setType(StaffType.AI);
        agent.setPhoneExtension("1002");
        agent.setSystemPrompt("You are Max, a friendly tier-1 support agent. Be concise and helpful.");
        agent.setModel("gpt-4o-mini");
        agent.setConnector(ConnectorType.NONE);
        staff.save(agent);

        // --- a human operator example ---
        Staff human = new Staff();
        human.setFullName("Jordan Lee");
        human.setEmail("jordan@example.com");
        human.setPosition("Operations Manager");
        human.setFunction("Human Operator");
        human.setDepartment(sales);
        human.setLevel(lead);
        human.setType(StaffType.HUMAN);
        human.setAlwaysOnline(false);
        staff.save(human);

        System.out.println("""

            ============================================================
             AI-Workforce seeded.
             Default admin login:  username = admin   password = admin
             (You must change this password at first login.)
             Sample company 'Knatware AI Company Ltd' created with AI staff.
            ============================================================
            """);
    }
}
