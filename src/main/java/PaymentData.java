public class PaymentData {
    private int customerId;
    private String movieId;
    private String saleDate; // Assuming the client sends the date as a String

    // Getters for Gson mapping
    public int getCustomerId() { return customerId; }
    public String getMovieId() { return movieId; }
    public String getSaleDate() { return saleDate; }
    
    // Setters
    public void setCustomerId(int customerId) { this.customerId = customerId; }
    public void setMovieId(String movieId) { this.movieId = movieId; }
    public void setSaleDate(String saleDate) { this.saleDate = saleDate; }
}
