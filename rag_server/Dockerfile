FROM python:3.10-slim

WORKDIR /app

# Cài thư viện
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Copy file cần
COPY app.py .
COPY load_to_chroma.py .

EXPOSE 8000

# Không khởi động Flask khi build
CMD ["python", "app.py"]
