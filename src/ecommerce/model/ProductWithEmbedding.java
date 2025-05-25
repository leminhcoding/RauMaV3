package ecommerce.model;

public class ProductWithEmbedding {

    private String tenSanPham;
    private String moTaSanPham;
    private String anh;
    private String gia;
    private String loaiSanPham;
    private String diemDanhGiaTrungBinh;
    private String soLuotDanhGia;
    private String nguonDuLieu;
    private float[] embedding;

    // ✅ Constructor đầy đủ để gán giá trị cho các field
    public ProductWithEmbedding(String tenSanPham, String moTaSanPham, String anh,
                                String gia, String loaiSanPham,
                                String diemDanhGiaTrungBinh, String soLuotDanhGia,
                                String nguonDuLieu, float[] embedding) {
        this.tenSanPham = tenSanPham;
        this.moTaSanPham = moTaSanPham;
        this.anh = anh;
        this.gia = gia;
        this.loaiSanPham = loaiSanPham;
        this.diemDanhGiaTrungBinh = diemDanhGiaTrungBinh;
        this.soLuotDanhGia = soLuotDanhGia;
        this.nguonDuLieu = nguonDuLieu;
        this.embedding = embedding;
    }

    public String getTenSanPham() {
        return tenSanPham;
    }

    public String getAnh() {
        return anh;
    }

    public String getGia() {
        return gia;
    }

    public String getLoaiSanPham() {
        return loaiSanPham;
    }

    public float[] getEmbedding() {
        return embedding;
    }

    // ✅ Trả về rating dạng double từ chuỗi
    public double getRating() {
        try {
            return Double.parseDouble(diemDanhGiaTrungBinh);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    // ✅ Trả về số lượt đánh giá dạng int từ chuỗi
    public int getReviewCount() {
        try {
            return Integer.parseInt(soLuotDanhGia.replaceAll("\\D", ""));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    // ✅ Chuyển sang Product (dùng cho UI)
    public Product toProduct() {
        return new Product(
                tenSanPham,
                anh,
                gia,
                moTaSanPham,
                diemDanhGiaTrungBinh,
                soLuotDanhGia,
                nguonDuLieu,
                loaiSanPham,
                loaiSanPham
        );
    }
}
