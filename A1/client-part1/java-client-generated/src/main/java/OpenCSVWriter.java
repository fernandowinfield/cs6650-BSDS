import com.opencsv.CSVWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class OpenCSVWriter {
  private String filePath;
  private List<String[]> dataRows;
  private File csvFile;
  private CSVWriter csvWriter;

  public OpenCSVWriter(String filePath) {
    this.filePath = filePath;
    this.csvFile = new File(filePath);
    try {
      this.csvWriter = new CSVWriter(new FileWriter(this.csvFile, true));
    } catch (IOException e) {
      System.out.println("Error initializing OpenCSV");
    }
    this.dataRows = new ArrayList<>();
  }

  public void writeDataRowToCsv(String[] dataRow) {
    for (String s: dataRow) {
      System.out.println(s);
    }
    this.csvWriter.writeNext(dataRow);
  }

  public void writeAllDataRowsToCsv() {
    this.csvWriter.writeAll(this.dataRows);
  }

  synchronized public void saveDataRow(String[] dataRow) {
    this.dataRows.add(dataRow);
  }

  public void closeWriter() {
    try {
      this.csvWriter.close();
    } catch (IOException e) {
      System.out.println("Error while trying to close the CSVWriter");
    }
  }

  public List<String[]> getDataRows() {
    return this.dataRows;
  }
}
