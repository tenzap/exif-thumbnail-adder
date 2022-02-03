package pixy.meta.adobe;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import pixy.meta.adobe.ImageResourceID;
import pixy.meta.adobe.Slice;
import pixy.meta.adobe._8BIM;

public class Slices extends _8BIM {
	List<Slice> slices;
	
	public Slices() {
		this("Slices");
	}
	
	public Slices(String name) {
		super(ImageResourceID.SLICES, name, null);
	}

	public Slices(String name, byte[] data) {
		super(ImageResourceID.SLICES, name, data);
		read();
	}
	
	public List<Slice> getSlices() {
		return slices;
	}
	
	public void print() {
		super.print();
	
	}

	private void read() {
		
	}
	
	public void write(OutputStream os) throws IOException {
		if(data == null) {
		
			size = data.length;
		}
		super.write(os);
	}
	
	public static final class SliceHeader {
		
	}
}
