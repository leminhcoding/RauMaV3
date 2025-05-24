from flask import Flask, request, jsonify
import chromadb
from chromadb.utils.embedding_functions import SentenceTransformerEmbeddingFunction

app = Flask(__name__)

# Dùng local ChromaDB client
client = chromadb.HttpClient(host="host.docker.internal", port=8001)

embedding_fn = SentenceTransformerEmbeddingFunction(
    model_name="VoVanPhuc/sup-SimCSE-VietNamese-phobert-base"
)
collection = client.get_or_create_collection(
    name="products", embedding_function=embedding_fn
)

@app.route("/embed", methods=["POST"])
def embed_and_search():
    data = request.json
    query = data["query"]

    # ✅ Danh sách danh mục cố định
    categories = ["Tủ lạnh", "Máy giặt", "Tivi", "Điều hòa"]

    # ✅ Tự bắt danh mục từ query người dùng
    matched_category = next((cat for cat in categories if cat.lower() in query.lower()), None)

    print("📌 Danh mục được phát hiện:", matched_category)

    # ✅ Truy vấn với bộ lọc nếu có
    if matched_category:
        results = collection.query(
            query_texts=[query],
            n_results=20,
            where={"Loại sản phẩm": matched_category}
        )
    else:
        results = collection.query(query_texts=[query], n_results=12)

    return jsonify(results)


@app.route("/add", methods=["POST"])
def add_products():
    data = request.json

    valid_products = [
        p for p in data["products"]
        if "Mô tả sản phẩm" in p and isinstance(p["Mô tả sản phẩm"], str) and p["Mô tả sản phẩm"].strip()
    ]

    collection.add(
        documents=[p["Mô tả sản phẩm"] for p in valid_products],
        metadatas=valid_products,
        ids=[str(p["id"]) for p in valid_products],
    )
    return jsonify({"status": "ok", "added": len(valid_products)})

if __name__ == "__main__":
    app.run(host="0.0.0.0", port=8000)
