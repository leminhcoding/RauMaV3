# Hướng Dẫn Cài Đặt & Chạy Ứng Dụng
## Bước 1: Cài đặt Docker
- Truy cập: https://www.docker.com/products/docker-desktop
- Tải Docker Desktop phù hợp hệ điều hành
- **Sau khi cài xong, KHỞI ĐỘNG Docker Desktop và để chạy ngầm**

## Bước 2: Vào thư mục chứa mã Python
```bash
cd rag_server
```

## Bước 3: Build image
```bash
docker build -t rag-server .
```

## Bước 4: Chạy ChromaDB bằng Docker (port 8001)
```bash
docker run -d -p 8001:8000 ghcr.io/chroma-core/chroma:latest
```

## Bước 5: Chạy Flask server (port 8000)
```bash
python app.py
```

## Bước 6: Gửi dữ liệu sản phẩm vào ChromaDB 
```bash
python load_to_chroma.py
```

## Bước 7: Chạy ứng dụng JavaFX
```bash
Mở MainApp.java trong src/ecommerce
```
