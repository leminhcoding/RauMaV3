import sys
from sentence_transformers import SentenceTransformer

query = sys.argv[1]
model = SentenceTransformer("all-MiniLM-L6-v2")
embedding = model.encode([query])[0]

# ✅ In từng số dưới dạng float chuẩn
print(",".join(str(float(v)) for v in embedding))
