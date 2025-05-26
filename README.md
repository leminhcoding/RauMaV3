## 1. System Requirements

To run the application, please make sure your machine has the following installed:

- Java JDK 17 or higher
- Python 3.10+
- Docker Desktop
- *(Recommended)* IntelliJ IDEA or VS Code for running the Java application

---

## 2. Setup and Execution Instructions

### 🛠️ Step 1: Start the backend

Open a terminal and run:

```bash
cd rag_server
docker-compose up --build
```
Once the services are running, open a new terminal and run:

```bash
python load_to_chroma.py
```
### 💻 Step 2: Launch the frontend
Open the file MainApp.java in the folder src/ecommerce/

Click Run to start the JavaFX application.