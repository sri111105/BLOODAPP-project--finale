# 🩸 Blood Donor & Emergency Assistance System

> [!WARNING]
> **Work In Progress**: This project is currently under active development. Features and interfaces may change as the work is still ongoing.

## 🚀 Overview
The **Blood Donor & Emergency Assistance System** is an AI-powered platform designed to seamlessly connect hospitals, emergency services, and individuals in need with voluntary blood donors. By leveraging modern web technologies, the system aims to streamline the blood donation process, reduce critical wait times during emergencies, and save lives.

## ✨ Features
- **Donor Registration**: Donors can submit their details natively through an easy-to-use form.
- **Hospital Dashboard**: Dedicated interfaces for hospitals to view, filter, search, and manage registered donors in real-time.
- **RESTful API**: Full CRUD capabilities handling data flow securely using Spring Boot.
- **Live Database Integration**: Real-time storage of donor details using MySQL.

## 🛠️ Technology Stack
- **Backend**: Java 17+, Spring Boot, Spring Data JPA
- **Frontend**: HTML5, Vanilla JavaScript, Bootstrap 5
- **Database**: MySQL Server
- **Build Tool**: Maven

---

## 💻 Getting Started (For Recruiters & Developers)

Follow these instructions to get a copy of the project up and running on your local machine.

### 1. Prerequisites
You will need the following installed on your system:
- **Java 17** or higher
- **MySQL Server** (Running locally on default port `3306`)
- **Git**

### 2. Database Setup (Important)
Before running the application, you must create the database it connects to:
1. Open your MySQL client (e.g., MySQL Workbench or Command Line).
2. Run the following SQL command to create the required database:
   ```sql
   CREATE DATABASE blood_db;
   ```
3. *Note*: The application expects the MySQL credentials to be Username: `root` and Password: `root@123`. 
   > If your MySQL credentials are different, open `src/main/resources/application.properties` and update `spring.datasource.username` and `spring.datasource.password` to match your local setup.

### 3. Running the Application
The easiest way to start the application is using the provided startup script:

**Windows**:
Double-click the `setup_and_run.bat` file in the root directory, or run it via terminal:
```bash
.\setup_and_run.bat
```

**Manual Maven Start (Any OS)**:
If you prefer running it manually via Maven wrapper:
```bash
./mvnw clean spring-boot:run
```

### 4. Viewing the Application
Once the Spring Boot server has started (you will see `Tomcat started on port 8081` in the console), open your web browser and navigate to:
- **Main Portal**: [http://localhost:8081/index.html](http://localhost:8081/index.html)
- **Hospital Dashboard**: [http://localhost:8081/hospital.html](http://localhost:8081/hospital.html)

## 🤝 Contributing
Since the project is currently a **Work in Progress**, contributions, suggestions, and feedback are highly appreciated. 

## 📝 License
This project is licensed under the MIT License.
