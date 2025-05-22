# JADE Spring Boot Medical Demo

This project is a demonstration of a multi-agent system using JADE (Java Agent DEvelopment Framework) integrated with Spring Boot. It simulates a basic medical appointment system.

## Technologies Used

*   Java 17
*   Spring Boot 3.x.x (Check `pom.xml` for specific version)
*   JADE 4.6.0 (or latest stable version found)
*   Maven

## Project Purpose

The system simulates interactions between `Patient`, `Receptionnist`, and `Medecin` (Doctor) agents:
*   Patients can request medical consultations.
*   Receptionists manage patient registration and consultation requests, coordinating with doctors.
*   Doctors manage their availability, accept/refuse consultations, and provide diagnostics.

## Agent Architecture

*   **Patient Agent:** Represents a patient who can request consultations.
*   **Receptionnist Agent:** Acts as a central coordinator. Manages patient information, consultation scheduling, and communication between patients and doctors.
*   **Medecin Agent:** Represents a doctor with a specific specialty. Manages their schedule, confirms appointments, and issues diagnostics.

Agents communicate using FIPA-compliant ACL messages and a custom `MedicalOntology`.

## How to Build

To build the project, ensure you have Java 17 and Maven installed. Navigate to the project root directory and run:

```bash
mvn clean install
```

## How to Run

After a successful build, you can run the Spring Boot application:

```bash
java -jar target/demo-0.0.1-SNAPSHOT.jar
```
(Adjust the JAR file name based on the version in `pom.xml`).

Upon startup, the application will initialize the JADE platform and the defined agents.

## JADE MTP Configuration

The JADE platform can be configured for various Message Transport Protocols (MTPs). By default, it often uses HTTP. If you need to configure specific hostnames or ports for JADE communication (e.g., for inter-platform communication), this is typically done through JADE platform profiles or configuration options passed at startup. The files `APDescription.txt` and `MTPs-Main-Container.txt` were present in the original project and seemed to relate to a specific JADE MTP setup with a hardcoded hostname; they have been removed for a more general demo setup. Refer to JADE documentation for advanced MTP configuration.
