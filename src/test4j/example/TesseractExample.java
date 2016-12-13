package test4j.example;

import java.awt.*;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

import javax.imageio.ImageIO;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import net.sourceforge.tess4j.*;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
/**
 * @author VAN SY
 *
 */
public class TesseractExample {
	static int num_phone = 0;
	static ArrayList<PhoneInfor> final_phone;
    public static void main(String[] args) {
        //System.setProperty("jna.library.path", "32".equals(System.getProperty("sun.arch.data.model")) ? "win32-x86" : "win32-x86-64");
    	URL well_market = null;
    	Document doc = null;
    	Elements page_elements = null;
    	String page_element = null;
    	int id_page = 1;
    	
    	final_phone  = new ArrayList<PhoneInfor>();
    	while(true) {
			try {
				well_market = new URL("https://www.chotot.com/quang-nam-da-nang/quang-nam/mua-ban-nha-dat?f=c&o="+(id_page++));
				try {
					doc = Jsoup.connect(well_market.toString()).timeout(0).get();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					break;
				}
				
				page_elements = doc.select("div.chotot-list-row");
				
				if (page_elements.size() == 0) {
					break;
				}
				
				for (int i = 0; i < page_elements.size(); i++) {
					//System.out.println(page_elements.get(i));
					page_element = page_elements.get(i).select("div > div > div > a").first().attr("href");
					getImageSrc(page_element);
				}
			} catch (MalformedURLException e3) {
				// TODO Auto-generated catch block
				break;
			}
    	}
    	
    	try {
			saveData();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
    
    private static BufferedImage scaleImage(BufferedImage img, int width, int height,
            Color background) {
        int imgWidth = img.getWidth();
        int imgHeight = img.getHeight();
        if (imgWidth*height < imgHeight*width) {
            width = imgWidth*height/imgHeight;
        } else {
            height = imgHeight*width/imgWidth;
        }
        BufferedImage newImage = new BufferedImage(width, height,
                BufferedImage.TYPE_INT_RGB);
        Graphics2D g = newImage.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.setBackground(background);
            g.clearRect(0, 0, width, height);
            g.drawImage(img, 0, 0, width, height, null);
        } finally {
            g.dispose();
        }
        return newImage;
    }
    
    private static void getImageSrc(String well_market) {
        URL imageURL = null;
		Document doc = null;
        String address;
        String province;
        Boolean is_company;  
        String name;
        String phone_number;
		
		try {
			doc = Jsoup.connect(well_market).timeout(0).get();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        
        Element phone_link = doc.select("img.AdPhonenum").first();
        String relSrc = phone_link.attr("abs:src");
        
        Element name_link = doc.select("a.advertised_user").first();
        name = name_link.text();
        
        try {
            Element company_link = doc.select("span.price_content_span").first();
            if (company_link.text().contains("công ty")) {
            	is_company = true;
            } else {
            	is_company = false;
            }
        } catch (NullPointerException address_error) {
        	is_company = false;
        }

        try {
            Element address_link = doc.getElementsContainingText("Địa chỉ: ").last();
            address_link = address_link.select("div.adparam_long_item > strong").first();
            address = address_link.text();
        } catch (NullPointerException address_error) {
        	address = "";
        }

        try {
        	Element province_link = doc.getElementsContainingText("Tỉnh, thành, quận: ").last();
            province_link = province_link.select("div.adparam_item > strong").first();
            province = province_link.text();
        } catch (NullPointerException address_error) {
        	province = "";
        }
        
        try {
        	imageURL =  new URL(relSrc);
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			System.out.println("Image URL Error!");
		}
        num_phone = num_phone+1;
        System.out.println(num_phone);
        
        phone_number = tesseractRecognize(imageURL);
        
        PhoneInfor phone_infor  = new PhoneInfor();
        phone_infor.setName(name);
        phone_infor.setIs_company(is_company);
        phone_infor.setAddress(address);
        phone_infor.setProvince(province);
        phone_infor.setPhone_number(phone_number);
        final_phone.add(phone_infor);
    }
    
    private static String tesseractRecognize(URL ImageSrc) {
    	BufferedImage phone_image = null;
    	String result = null;
    	
    	try {
    		phone_image = ImageIO.read(ImageSrc);
		} catch (MalformedURLException e2) {
			e2.printStackTrace();
		} catch (IOException e2) {
			System.out.println("Cannot connect to server");
			return null;
			//e2.printStackTrace();
		}
    	
    	phone_image = scaleImage(phone_image,150,16, Color.WHITE);
        ITesseract instance = new Tesseract();

        try {
			result = instance.doOCR(phone_image);
        } catch (TesseractException e) {
            System.err.println(e.getMessage());
        }
        
        result = result.replaceAll("O", "0");
        result = result.replaceAll("l", "1");
		result = result.replaceAll("]", "1");
		result = result.replaceAll("L", "1");
        result = result.replaceAll(" ", "");
        result = result.trim();
        //result = result.replaceAll(".", "");
        
    	return result;
    }
    
    private static void saveData() throws FileNotFoundException, IOException {
    	Workbook wb = new XSSFWorkbook();
        Sheet sheet = wb.createSheet("new sheet");

        // Create a row and put some cells in it. Rows are 0 based.
        Row row = sheet.createRow((short)0);
        // Create a cell and put a value in it.
        Cell cell = row.createCell((short)0);
        cell.setCellValue("TÊN");
        row.createCell(1).setCellValue("CÁ NHÂN/CÔNG TY");
        row.createCell(2).setCellValue("ĐỊA CHỈ");
        row.createCell(3).setCellValue("TỈNH/THÀNH PHỐ");
        row.createCell(4).setCellValue("SỐ ĐIỆN THOẠI");
        
        for (int i = 0; i < final_phone.size(); i++) {
        	row = sheet.createRow(i+1);
        	row.createCell((short)0).setCellValue(final_phone.get(i).getName());
        	
        	if (final_phone.get(i).getIs_company()) {
        		row.createCell(1).setCellValue("Công ty");
        	} else {
        		row.createCell(1).setCellValue("Cá nhân");
        	}
        	
            row.createCell(2).setCellValue(final_phone.get(i).getAddress());
            row.createCell(3).setCellValue(final_phone.get(i).getProvince());
            row.createCell(4).setCellValue(final_phone.get(i).getPhone_number());
		}

        // Write the output to a file
        FileOutputStream fileOut = new FileOutputStream("phone_list.xlsx");
        wb.write(fileOut);
        fileOut.close();
	}
}