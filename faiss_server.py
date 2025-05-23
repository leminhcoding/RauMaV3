import faiss
import numpy as np
import json
from flask import Flask, request, jsonify

app = Flask(__name__)

# Load product data
with open("src/main/resources/embedded_products.json", "r", encoding="utf-8") as f:
    data = json.load(f)

# Tạo danh sách embeddings và map index → sản phẩm
embeddings = np.array([item["embedding"] for item in data], dtype='float32')
products = data

# Normalize để dùng inner product ~ cosine similarity
embeddings /= np.linalg.norm(embeddings, axis=1, keepdims=True)

# Tạo FAISS index
index = faiss.IndexFlatIP(len(embeddings[0]))
index.add(embeddings)

@app.route("/search", methods=["POST"])
def search():
    req = request.get_json()
    query_vector = np.array(req["query"], dtype="float32")
    query_vector /= np.linalg.norm(query_vector)

    k = req.get("top_k", 10)
    D, I = index.search(np.expand_dims(query_vector, axis=0), k)
    result = [products[i] for i in I[0]]
    return jsonify(result)

if __name__ == "__main__":
    app.run(host='0.0.0.0', port=5000)
