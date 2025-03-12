# Library Management System 🤓📚

## 1. Introduction

### 1.1 Purpose
The purpose of this document is to define the functional and non-functional requirements for the Library Management System. The Library Management System is a desktop-based application for librarians to manage library operations efficiently. It includes book checkouts, returns, cataloging, member management, due date tracking, fines for late returns, and a search functionality for finding books, while ensuring security against potential threats.

### 1.2 Scope
The Library Management System is intended for library staff and administrators to manage internal operations, including book lending, cataloging, and user account management. The system must also implement security measures to prevent unauthorized access and potential abuse.

<br/>

## 2. Non-Functional Requirements

- Security:	The system should ensure security against potential threats.
- Performance:	The system should process transactions within 2 seconds.
- Usability:	The system should provide an intuitive UI for staff users.
- Availability:	The system must be available 99.9% of the time.


## 3. Use Case Diagram
- You can find the SRS [here](https://github.com/3bdop/Library-Management-System/blob/main/documents/Library_Management_System_SRS.pdf).

  [comment]: <![image](https://github.com/user-attachments/assets/6e70ed5b-f902-46cb-966f-98a0ab8f4572)>

## 4. High Level Data Flow Diagram
- You can find the SDD [here](https://github.com/3bdop/Library-Management-System/blob/main/documents/Library_Management_System_SDD.pdf).
  
[comment]: <![image](https://github.com/user-attachments/assets/302d8187-f543-465f-a438-b78c7f23f426)>

<br/>

# Installation 🛠️

Follow these steps to get the project up and running:

1. **Clone the repository:**

   ```
   git clone https://github.com/3bdop/Library-Management-System.git
   ```

2. **Setup Application Configration:**
     - Add new application configration.
     - Select the App as the main class.
     - Modify options --> Add VM options.
     - Add the following for VM options:
       ```
       --module-path C:\javafx-sdk-23.0.1\lib
       --add-modules javafx.controls,javafx.fxml,javafx.graphics
       --add-exports=javafx.graphics/com.sun.javafx.util=ALL-UNNAMED
       --add-exports=javafx.base/com.sun.javafx.reflect=ALL-UNNAMED
       ```
   
