import requests
import json
import os

# Xác định đường dẫn JSON
BASE_DIR = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
DATA_PATH = os.path.join(BASE_DIR, "resources", "product_texts.json")

# Đọc file JSON
with open(DATA_PATH, "r", encoding="utf-8") as f:
    data = json.load(f)

# Lọc sản phẩm có mô tả
products = [
    p for p in data
    if "Mô tả sản phẩm" in p and isinstance(p["Mô tả sản phẩm"], str) and p["Mô tả sản phẩm"].strip()
]

# Chuẩn hóa key danh mục cho chắc chắn (nếu có sai lệch viết hoa/thường)
for p in products:
    if "Loại sản phẩm" not in p and "loai_san_pham" in p:
        p["Loại sản phẩm"] = p["loai_san_pham"]

# Gán ID nếu chưa có
for i, p in enumerate(products):
    p["id"] = i + 1

print(f"✅ Tổng số sản phẩm gửi: {len(products)}")

# Gửi lên Flask server
response = requests.post(
    "http://localhost:8000/add",
    json={"products": products}
)

try:
    print("📨 Response:", response.status_code)
    print("🔁 Nội dung:", response.json())
except Exception:
    print("❌ Không thể parse JSON. Nội dung raw:")
    print(response.text)
