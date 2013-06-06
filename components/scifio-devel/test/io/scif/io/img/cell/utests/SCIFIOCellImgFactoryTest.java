/*
 * #%L
 * OME SCIFIO package for reading and converting scientific file formats.
 * %%
 * Copyright (C) 2005 - 2013 Open Microscopy Environment:
 *   - Board of Regents of the University of Wisconsin-Madison
 *   - Glencoe Software, Inc.
 *   - University of Dundee
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * 
 * The views and conclusions contained in the software and documentation are
 * those of the authors and should not be interpreted as representing official
 * policies, either expressed or implied, of any organization.
 * #L%
 */

package io.scif.io.img.cell.utests;

import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.fail;
import io.scif.Reader;
import io.scif.SCIFIO;
import io.scif.io.img.cell.SCIFIOCellImgFactory;

import java.io.IOException;

import org.scijava.Context;
import org.testng.annotations.Test;

/**
 * Unit tests for {@link SCIFIOCellImgFactory}.
 * <p>
 * Runs through the following tests:
 * uint8, RGB, int16
 * </p>
 * <p>
 * For image sizes: 512x512, 4096x4096
 * </p>
 * Tests expected cell sizes vs.
 * {@link SCIFIOCellImgFactory#getOptimalCellXY(Reader, int, int)}
 * for various tile sizes relative to the image.
 * </p>
 * <p>
 * Note: RGB output sizes should be the same as uint8 because
 * the RGB channel count is not taken into consideration for plane size,
 * because {@link ChannelSeparator} is assumed.
 * </p>
 * 
 * @author Mark Hiner hinerm at gmail.com
 *
 */
@Test(groups="cellTests")
public class SCIFIOCellImgFactoryTest {
  
  // -- Context fields --
  
  private Context ctx;
  private SCIFIO scifio;
  
  // -- Constructor --
  
  public SCIFIOCellImgFactoryTest() {
    ctx = new Context();
    scifio = new SCIFIO(ctx);
  }

  // -- Tests --

  @Test
  public void cellSmallSizePlaneTest() {
    String id = "cellTest&sizeX=13&sizeY=17.fake";
    Reader reader = getReader(id);

    // test that small image plane tile size matches plane size
    testCellXY(reader, 13, 17, 13, 17);

    // test that bogus optimal tile sizes do not screw things up
    // (tile should never be larger than the image plane itself)
    testCellXY(reader, 128, 128, 13, 17); // too big
    testCellXY(reader, 0, 0, 13, 17); // zero is bogus
    testCellXY(reader, -1, -1, 13, 17); // negative
    testCellXY(reader, Integer.MIN_VALUE, Integer.MAX_VALUE, 13, 17); // argh
    testCellXY(reader, Integer.MAX_VALUE, Integer.MIN_VALUE, 13, 17); // height/width bias

    close(reader);
  }

  @Test
  public void cellTileLayoutTest() {
    String id = "cellTest&sizeX=2468&sizeY=1817.fake";
    Reader reader = getReader(id);

    // Notably, a 3 x 3 tile structure (1320 x 1530) would be closer to
    // 2MB, it would not span the entire plane width, which is undesirable.
    // So a 5+ x 1 tile structure (2468 x 510) should be what we get here.
    testCellXY(reader, 440, 510, 2468, 510);

    close(reader);
  }
  
  @Test
  public void cellMediumPlaneSizeTest() {
    String id = "cellTest&sizeX=512&sizeY=512.fake";
    Reader reader = getReader(id);
    
    // Tile == image size, should come back unchanged
    testCellXY(reader, 512, 512, 512, 512);
    
    // Expanding tiles vertically
    testCellXY(reader, 512, 16, 512, 512);
    
    // expanding tiles horizontally
    testCellXY(reader, 16, 512, 512, 512);
    
    // small expansion, non-factor
    testCellXY(reader, 511, 511, 512, 512);
    
    // non-factor expansion, vertical
    testCellXY(reader, 511, 15, 512, 512);
    
    // non-factor expansion, horizontal
    testCellXY(reader, 15, 511, 512, 512);
    
    // mixed factor/non-factor expansion, horizontal
    testCellXY(reader, 511, 16, 512, 512);
    
    // small tile expansion
    testCellXY(reader, 16, 16, 512, 512);
    
    // small tile, mixed factor/non-factor
    testCellXY(reader, 16, 15, 512, 512);
    
    close(reader);
  }
  
  @Test
  public void cellMediumPlaneSizeRGBTest() {
    String id = "cellTest&sizeX=512&sizeY=512&RGB=3.fake";
    Reader reader = getReader(id);
    
    // NB: All these tests should be identical as 
    // cellMediumPlaneSizeTest. The fact that this
    // image is RGB shouldn't affect any tile size
    // calculations, because ChannelSeparator use
    // is assumed.
    testCellXY(reader, 512, 512, 512, 512);
    testCellXY(reader, 512, 16, 512, 512);
    testCellXY(reader, 16, 512, 512, 512);
    testCellXY(reader, 511, 511, 512, 512);
    testCellXY(reader, 511, 15, 512, 512);
    testCellXY(reader, 15, 511, 512, 512);
    testCellXY(reader, 511, 16, 512, 512);
    testCellXY(reader, 16, 16, 512, 512);
    testCellXY(reader, 16, 15, 512, 512);
    
    close(reader);
  }
  
  @Test
  public void cellMediumPlaneSize16BitTest() {
    String id = "cellTest&sizeX=512&sizeY=512&pixelType=int16.fake";
    Reader reader = getReader(id);
    
    // NB: These tests are identical to 
    // cellMediumPlaneSizeTest.
    // 512 x 512 x 2 bytes per pixel should
    // still be under 2MB, so the whole image
    // will always be returned as the 
    // optimal cell size.
    testCellXY(reader, 512, 512, 512, 512);
    testCellXY(reader, 512, 16, 512, 512);
    testCellXY(reader, 16, 512, 512, 512);
    testCellXY(reader, 511, 511, 512, 512);
    testCellXY(reader, 511, 15, 512, 512);
    testCellXY(reader, 15, 511, 512, 512);
    testCellXY(reader, 511, 16, 512, 512);
    testCellXY(reader, 16, 16, 512, 512);
    testCellXY(reader, 16, 15, 512, 512);
    
    close(reader);
  }
  
  @Test
  public void cellLargePlaneSizeTest() {
    String id = "cellTest&sizeX=4096&sizeY=4096.fake";
    Reader reader = getReader(id);
    
    // even factor plane, expands horizontally
    testCellXY(reader, 512, 512, 4096, 512);
    
    // even factor horizontal strip. expands horizontally
    // first, then vertically.
    testCellXY(reader, 512, 16, 4096, 512);
    
    // Same as above even though strips are vertical
    testCellXY(reader, 16, 512, 4096, 512);
    
    // Unit vertical strips. Should expand horizontally
    testCellXY(reader, 1, 4096, 512, 4096);
    
    // Unit horizontal strips. Should expand vertically.
    testCellXY(reader, 4096, 1, 4096, 512);
    
    // Small tile. Expands horizontally first, then vertically
    testCellXY(reader, 16, 16, 4096, 512);
    
    close(reader);
  }

  @Test
  public void cellLargePlaneSizeRGBTest() {
    String id = "cellTest&sizeX=4096&sizeY=4096&RGB=3.fake";
    Reader reader = getReader(id);
    
    // NB: These tests are identical to 
    // cellLargePlaneSizeTest. The fact that this
    // image is RGB shouldn't affect any tile size
    // calculations, because ChannelSeparator use
    // is assumed.
    testCellXY(reader, 512, 512, 4096, 512);
    testCellXY(reader, 512, 16, 4096, 512);
    testCellXY(reader, 16, 512, 4096, 512);
    testCellXY(reader, 1, 4096, 512, 4096);
    testCellXY(reader, 4096, 1, 4096, 512);
    testCellXY(reader, 16, 16, 4096, 512);
    
    close(reader);
  }
  
  @Test
  public void cellLargePlaneSize16BitTest() {
    String id = "cellTest&sizeX=4096&sizeY=4096&pixelType=int16.fake";
    Reader reader = getReader(id);
    
    // NB: These are the same tile sizes as
    // the cellLargePlaneSizeTest. However,
    // the 2 bytes per pixel will change how
    // the cell sizes are calculated
    
    // Expands horizontally. The 512 height
    // limits expansion
    testCellXY(reader, 512, 512, 2048, 512);
    
    // Expands horizontally first, then vertically
    testCellXY(reader, 512, 16, 4096, 256);
    
    // Expands horizontally. The 512 height
    // limits expansion
    testCellXY(reader, 16, 512, 2048, 512);
    
    // expands horizontally as much as possible
    testCellXY(reader, 1, 4096, 256, 4096);
    
    // expands vertically as much as possible
    testCellXY(reader, 4096, 1, 4096, 256);
    
    // expands horizontally first, then vertically
    testCellXY(reader, 16, 16, 4096, 256);
    
    close(reader);
  }

  // -- Helper methods --
  
  // Tests the cell sizes given the indicated tileX and tileY sizes,
  // and compares to the target cell sizes
  private void testCellXY(Reader r, int tileX, int tileY,
      int targetCellX, int targetCellY)
  {
    int[] cellXY = SCIFIOCellImgFactory.getOptimalCellXY(r, tileX, tileY);
    assertEquals(targetCellX, cellXY[0]);
    assertEquals(targetCellY, cellXY[1]);
  }
  
  // Gets a reader for the given id, failing on exception
  private Reader getReader(String id) {
    Reader r = null;
    
    try {
      r =  scifio.initializer().initializeReader(id);
    } catch(Exception e) { 
      fail(e.getMessage());
    }
    
    return r;
  }
  
  // Closes the given reader, failing on exception
  private void close(Reader r) {
    try {
      r.close();
    } catch (IOException e) {
      fail(e.getMessage());
    }
  }
}
