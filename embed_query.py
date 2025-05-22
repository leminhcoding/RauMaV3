import sys
from transformers import AutoModel, AutoTokenizer
from sentence_transformers import SentenceTransformer, models

# Mô hình tiếng Việt tối ưu cho ngữ nghĩa
model_name = "VoVanPhuc/sup-SimCSE-VietNamese-phobert-base"
word_embedding_model = models.Transformer(model_name)
pooling_model = models.Pooling(word_embedding_model.get_word_embedding_dimension())
model = SentenceTransformer(modules=[word_embedding_model, pooling_model])

# Nhận truy vấn từ dòng lệnh (từ Java truyền vào)
query = sys.argv[1] if len(sys.argv) > 1 else "tủ lạnh tiết kiệm điện cho 5 người"

# Sinh embedding từ truy vấn
embedding = model.encode(query)

# In ra 1 dòng duy nhất — Java sẽ đọc dòng này để lấy vector
print(embedding.tolist())
