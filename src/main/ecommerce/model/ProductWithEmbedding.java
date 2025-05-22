package main.ecommerce.model;

import com.google.gson.annotations.SerializedName;

public class ProductWithEmbedding {

    @SerializedName("Tên sản phẩm")
    private String tenSanPham;

    @SerializedName("Mô tả sản phẩm")
    private String moTaSanPham;

    @SerializedName("Ảnh")
    private String anh;

    @SerializedName("Giá")
    private String gia;

    @SerializedName("Loại sản phẩm")
    private String loaiSanPham;

    @SerializedName("Điểm đánh giá trung bình")
    private String diemDanhGiaTrungBinh;

    @SerializedName("Số lượt đánh giá")
    private String soLuotDanhGia;

    @SerializedName("Nguồn dữ liệu")
    private String nguonDuLieu;

    private float[] embedding;

    public String getTenSanPham() {
        return tenSanPham;
    }

    public String getMoTaSanPham() {
        return moTaSanPham;
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

    public String getDiemDanhGiaTrungBinh() {
        return diemDanhGiaTrungBinh;
    }

    public String getSoLuotDanhGia() {
        return soLuotDanhGia;
    }

    public String getNguonDuLieu() {
        return nguonDuLieu;
    }

    public float[] getEmbedding() {
        return embedding;
    }

    // ✅ Thêm method toProduct()
    public Product toProduct() {
        return new Product(
                getTenSanPham(),
                getAnh(),
                getGia(),
                getMoTaSanPham(),
                getDiemDanhGiaTrungBinh(),
                getSoLuotDanhGia(),
                getNguonDuLieu(),
                getLoaiSanPham()
        );
    }
}
