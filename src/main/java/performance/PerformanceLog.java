package performance;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PerformanceLog {
    private static final String LOG_FILE_PATH = "/Users/meo/Desktop/UCI/cs122b/2025-fall-cs-122b-mt/Project_Fablix/log_sql.txt";

    public static synchronized void log(long ts, long tj) {
        // Validate inputs
        if (ts < 0 || tj < 0) {
            System.err.println("PerformanceLog WARNING: Invalid values - ts=" + ts + ", tj=" + tj);
            return;
        }

        System.out.println("PerformanceLog: Attempting to log TS=" + ts + ", TJ=" + tj + " to " + LOG_FILE_PATH);
        
        // Ensure file exists
        File logFile = new File(LOG_FILE_PATH);
        if (!logFile.exists()) {
            try {
                // Create parent directories if they don't exist
                File parentDir = logFile.getParentFile();
                if (parentDir != null && !parentDir.exists()) {
                    parentDir.mkdirs();
                }
                if (logFile.createNewFile()) {
                    System.out.println("PerformanceLog: Created new log file: " + LOG_FILE_PATH);
                }
            } catch (IOException e) {
                System.err.println("PERFORMANCE LOG ERROR: Failed to create log file: " + e.getMessage());
                e.printStackTrace();
                return;
            }
        }

        try (FileWriter fw = new FileWriter(LOG_FILE_PATH, true);
             PrintWriter pw = new PrintWriter(fw)) {
            pw.println(ts + "," + tj);
            pw.flush(); // Explicitly flush to ensure immediate write
            fw.flush(); // Also flush the underlying FileWriter
        } catch (IOException ex) {
            System.err.println("PERFORMANCE LOG ERROR: " + ex.getMessage());
            ex.printStackTrace();
        } catch (Exception ex) {
            System.err.println("PERFORMANCE LOG ERROR (Unexpected): " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    public static void main(String[] args) {
        System.out.println("Starting Performance Log" + LOG_FILE_PATH);

        List<Double> tsList = new ArrayList<>();
        List<Double> tjList = new ArrayList<>();

        File file = new File(LOG_FILE_PATH);
        if (!file.exists()) {
            try {
                if (file.createNewFile()) {
                    System.out.println("File not found. Created new file: " + LOG_FILE_PATH);
                }
            } catch (IOException e) {
                System.err.println("Error creating file: " + e.getMessage());
                return;
            }
        }

        try (BufferedReader br = new BufferedReader(new FileReader(LOG_FILE_PATH))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length == 2) {
                    try {
                        long tsNano = Long.parseLong(parts[0].trim());
                        long tjNano = Long.parseLong(parts[1].trim());

                        // Convert to Milliseconds (ms)
                        tsList.add(tsNano / 1_000_000.0);
                        tjList.add(tjNano / 1_000_000.0);
                    } catch (NumberFormatException e) {
                        System.err.println("Skipping invalid line: " + line);
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading file: " + e.getMessage());
            return;
        }

        if (tsList.isEmpty()) {
            System.out.println("Log file is empty or missing.");
            return;
        }

        Collections.sort(tsList);
        Collections.sort(tjList);

        // Print Report
        System.out.println("==========================================================");
        System.out.printf("Performance Report (Total Samples: %d)%n", tsList.size());
        System.out.println("==========================================================");
        System.out.printf("%-10s | %-15s | %-15s%n", "Metric", "Ts (ms)", "Tj (ms)");
        System.out.println("----------------------------------------------------------");

        printRow("Average", calculateAverage(tsList), calculateAverage(tjList));
        printRow("Median", calculateMedian(tsList), calculateMedian(tjList));
        printRow("Std Dev", calculateStdDev(tsList), calculateStdDev(tjList));

        System.out.println("==========================================================");
    }

    private static void printRow(String label, double tsVal, double tjVal) {
        System.out.printf("%-10s | %-15.4f | %-15.4f%n", label, tsVal, tjVal);
    }

    private static double calculateAverage(List<Double> data) {
        if (data.isEmpty()) return 0.0;
        double sum = 0;
        for (double val : data) {
            sum += val;
        }
        return sum / data.size();
    }

    private static double calculateMedian(List<Double> sortedData) {
        if (sortedData.isEmpty()) return 0.0;
        int size = sortedData.size();
        if (size % 2 == 1) {
            return sortedData.get(size / 2);
        } else {
            return (sortedData.get(size / 2 - 1) + sortedData.get(size / 2)) / 2.0;
        }
    }

    private static double calculateStdDev(List<Double> data) {
        if (data.isEmpty()) return 0.0;
        double mean = calculateAverage(data);
        double sumSquaredDiff = 0;
        for (double val : data) {
            sumSquaredDiff += Math.pow(val - mean, 2);
        }
        return Math.sqrt(sumSquaredDiff / data.size());
    }

    // Optional: Helper to clear the log file before a new test run
    public static void clearLog() {
        try (PrintWriter pw = new PrintWriter(LOG_FILE_PATH)) {
            pw.print(""); // Clear the file
            pw.flush(); // Ensure it's written immediately
        } catch (IOException e) {
            System.err.println("PERFORMANCE LOG ERROR: Failed to clear log file: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
