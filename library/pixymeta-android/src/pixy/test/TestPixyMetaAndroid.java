package pixy.test;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import android.graphics.Bitmap;
import pixy.meta.Metadata;
import pixy.meta.MetadataEntry;
import pixy.meta.MetadataType;
import pixy.meta.adobe.IPTC_NAA;
import pixy.meta.adobe._8BIM;
import pixy.meta.exif.Exif;
import pixy.meta.exif.ExifTag;
import pixy.meta.jpeg.JpegExif;
import pixy.meta.tiff.TiffExif;
import pixy.meta.iptc.IPTCApplicationTag;
import pixy.meta.iptc.IPTCDataSet;
import pixy.meta.jpeg.JPGMeta;
import pixy.meta.jpeg.JpegXMP;
import pixy.meta.xmp.XMP;
import pixy.image.tiff.FieldType;
import pixy.image.tiff.TiffTag;
import pixy.string.StringUtils;
import pixy.string.XMLUtils;
import pixy.util.MetadataUtils;

public class TestPixyMetaAndroid {
	// Obtain a logger instance
	private static final Logger LOGGER = LoggerFactory.getLogger(TestPixyMetaAndroid.class);
	
	public static void main(String[] args) throws Exception {
		new TestPixyMetaAndroid().test(args);
	}
	
	public void test(String ... args) throws Exception {
		Map<MetadataType, Metadata> metadataMap = Metadata.readMetadata(args[0]);
		LOGGER.info("Start of metadata information:");
		LOGGER.info("Total number of metadata entries: {}", metadataMap.size());
		
		int i = 0;
		for(Map.Entry<MetadataType, Metadata> entry : metadataMap.entrySet()) {
			//
			LOGGER.info("Metadata entry {} - {}", i, entry.getKey());
			Metadata meta = entry.getValue();
			if(meta instanceof XMP) XMP.showXMP((XMP)meta);
			else {
				Iterator<MetadataEntry> iterator = entry.getValue().iterator();
				
				while(iterator.hasNext()) {
					MetadataEntry item = iterator.next();
					printMetadata(item, "", "     ");
				}
			}			
			i++;
			LOGGER.info("-----------------------------------------");
		}
		LOGGER.info("End of metadata information.");

		FileInputStream fin = null;
		FileOutputStream fout = null;
		
		if(metadataMap.get(MetadataType.XMP) != null) {
			XMP xmp = (XMP)metadataMap.get(MetadataType.XMP);
			fin = new FileInputStream("images/1.jpg");
			fout = new FileOutputStream("1-xmp-inserted.jpg");
			JpegXMP jpegXmp = null;
			if(!xmp.hasExtendedXmp())
				jpegXmp = new JpegXMP(xmp.getData());
			else {
				Document xmpDoc = xmp.getXmpDocument();
				Document extendedXmpDoc = xmp.getExtendedXmpDocument();
				jpegXmp = new JpegXMP(XMLUtils.serializeToString(xmpDoc.getDocumentElement(), "UTF-8"), XMLUtils.serializeToString(extendedXmpDoc));
			}
			Metadata.insertXMP(fin, fout, jpegXmp);
			fin.close();
			fout.close();
		}
		
		Metadata.extractThumbnails("images/iptc-envelope.tif", "iptc-envelope");
	
		fin = new FileInputStream("images/iptc-envelope.tif");
		fout = new FileOutputStream("iptc-envelope-iptc-inserted.tif");
			
		Metadata.insertIPTC(fin, fout, createIPTCDataSet(), true);
		
		fin.close();
		fout.close();
		
		fin = new FileInputStream("images/wizard.jpg");
		fout = new FileOutputStream("wizard-iptc-inserted.jpg");
		
		Metadata.insertIPTC(fin, fout, createIPTCDataSet(), true);
		
		fin.close();
		fout.close();
		
		fin = new FileInputStream("images/1.jpg");
		fout = new FileOutputStream("1-irbthumbnail-inserted.jpg");
		
		Metadata.insertIRBThumbnail(fin, fout, createThumbnail("images/1.jpg"));
		
		fin.close();
		fout.close();
		
		fin = new FileInputStream("images/f1.tif");
		fout = new FileOutputStream("f1-irbthumbnail-inserted.tif");
		
		Metadata.insertIRBThumbnail(fin, fout, createThumbnail("images/f1.tif"));
		
		fin.close();
		fout.close();		

		fin = new FileInputStream("images/exif.tif");
		fout = new FileOutputStream("exif-exif-inserted.tif");
		
		Metadata.insertExif(fin, fout, populateExif(TiffExif.class), true);
		
		fin.close();
		fout.close();
		
		fin = new FileInputStream("images/12.jpg");
		fout = new FileOutputStream("12-exif-inserted.jpg");

		Metadata.insertExif(fin, fout, populateExif(JpegExif.class), true);
		
		fin.close();
		fout.close();
		
		fin = new FileInputStream("images/12.jpg");
		fout = new FileOutputStream("12-metadata-removed.jpg");
		
		Metadata.removeMetadata(fin, fout, MetadataType.JPG_JFIF, MetadataType.JPG_ADOBE, MetadataType.IPTC, MetadataType.ICC_PROFILE, MetadataType.XMP, MetadataType.EXIF);
		
		fin.close();
		fout.close();
		
		fin = new FileInputStream("images/12.jpg");
		fout = new FileOutputStream("12-photoshop-iptc-inserted.jpg");
		
		Metadata.insertIRB(fin, fout, createPhotoshopIPTC(), true);
		
		fin.close();
		fout.close();
		
		fin = new FileInputStream("images/table.jpg");
		JPGMeta.extractDepthMap(fin, "table");
		
		fin.close();
		
		fin = new FileInputStream("images/butterfly.png");
		fout = new FileOutputStream("comment-inserted.png");
		
		Metadata.insertComments(fin, fout, Arrays.asList("Comment1", "Comment2"));
		
		fin.close();
		fout.close();
	}
	
	private static List<IPTCDataSet> createIPTCDataSet() {
		List<IPTCDataSet> iptcs = new ArrayList<IPTCDataSet>();
		iptcs.add(new IPTCDataSet(IPTCApplicationTag.COPYRIGHT_NOTICE, "Copyright 2014-2016, yuwen_66@yahoo.com"));
		iptcs.add(new IPTCDataSet(IPTCApplicationTag.CATEGORY, "ICAFE"));
		iptcs.add(new IPTCDataSet(IPTCApplicationTag.KEY_WORDS, "Welcome 'icafe' user!"));
		
		return iptcs;
	}
	
	private static List<_8BIM> createPhotoshopIPTC() {
		IPTC_NAA iptc = new IPTC_NAA();
		iptc.addDataSet(new IPTCDataSet(IPTCApplicationTag.COPYRIGHT_NOTICE, "Copyright 2014-2016, yuwen_66@yahoo.com"));
		iptc.addDataSet(new IPTCDataSet(IPTCApplicationTag.KEY_WORDS, "Welcome 'icafe' user!"));
		iptc.addDataSet(new IPTCDataSet(IPTCApplicationTag.CATEGORY, "ICAFE"));
		
		return new ArrayList<_8BIM>(Arrays.asList(iptc));
	}
	
	private static Bitmap createThumbnail(String filePath) throws IOException {
		FileInputStream fin = null;
		try {
			fin = new FileInputStream(filePath);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		
		Bitmap thumbnail = MetadataUtils.createThumbnail(fin);
		
		fin.close();
		
		return thumbnail;
	}
	
	// This method is for testing only
	private static Exif populateExif(Class<?> exifClass) throws IOException {
		// Create an EXIF wrapper
		Exif exif = exifClass == (TiffExif.class)?new TiffExif() : new JpegExif();
		exif.addImageField(TiffTag.WINDOWS_XP_AUTHOR, FieldType.WINDOWSXP, "Author");
		exif.addImageField(TiffTag.WINDOWS_XP_KEYWORDS, FieldType.WINDOWSXP, "Copyright;Author");
		DateFormat formatter = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss");
		exif.addExifField(ExifTag.EXPOSURE_TIME, FieldType.RATIONAL, new int[] {10, 600});
		exif.addExifField(ExifTag.FNUMBER, FieldType.RATIONAL, new int[] {49, 10});
		exif.addExifField(ExifTag.ISO_SPEED_RATINGS, FieldType.SHORT, new short[]{273});
		//All four bytes should be interpreted as ASCII values - represents [0220] - new byte[]{48, 50, 50, 48}
		exif.addExifField(ExifTag.EXIF_VERSION, FieldType.UNDEFINED, "0220".getBytes());
		exif.addExifField(ExifTag.DATE_TIME_ORIGINAL, FieldType.ASCII, formatter.format(new Date()));
		exif.addExifField(ExifTag.DATE_TIME_DIGITIZED, FieldType.ASCII, formatter.format(new Date()));
		exif.addExifField(ExifTag.FOCAL_LENGTH, FieldType.RATIONAL, new int[] {240, 10});		
		// Insert ThumbNailIFD
		// Since we don't provide thumbnail image, it will be created later from the input stream
		exif.setThumbnailRequired(true);
		
		return exif;
	}
	
	private void printMetadata(MetadataEntry entry, String indent, String increment) {
		LOGGER.info(indent + entry.getKey() + (StringUtils.isNullOrEmpty(entry.getValue())? "" : ": " + entry.getValue()));
		if(entry.isMetadataEntryGroup()) {
			indent += increment;
			Collection<MetadataEntry> entries = entry.getMetadataEntries();
			for(MetadataEntry e : entries) {
				printMetadata(e, indent, increment);
			}			
		}
	}
}
