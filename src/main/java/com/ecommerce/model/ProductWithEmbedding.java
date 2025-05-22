package com.ecommerce.model;

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

    // Chuyển sang Product (hiển thị UI)
    public Product toProduct() {
        return new Product(
                tenSanPham,
                anh,
                gia,
                moTaSanPham,
                diemDanhGiaTrungBinh,
                soLuotDanhGia,
                nguonDuLieu,
                loaiSanPham
        );
    }
}
