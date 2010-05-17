//
// ColorDialog.java
//

/*
LOCI Plugins for ImageJ: a collection of ImageJ plugins including the
Bio-Formats Importer, Bio-Formats Exporter, Bio-Formats Macro Extensions,
Data Browser, Stack Colorizer and Stack Slicer. Copyright (C) 2005-@year@
Melissa Linkert, Curtis Rueden and Christopher Peterson.

This program is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 2 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/

package loci.plugins.in;

import ij.gui.GenericDialog;
import ij.util.Tools;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.Panel;
import java.awt.TextField;
import java.awt.event.TextEvent;
import java.awt.event.TextListener;
import java.util.ArrayList;
import java.util.List;

import loci.plugins.util.ImageProcessorReader;
import loci.plugins.util.WindowTools;

/**
 * Bio-Formats Importer custom color chooser dialog box.
 *
 * Heavily adapted from {@link ij.gui.ColorChooser}.
 * ColorChooser is not used because there is no way to change the slider
 * labels&mdash;this means that we can't record macros in which custom colors
 * are chosen for multiple channels.
 *
 * <dl><dt><b>Source code:</b></dt>
 * <dd><a href="https://skyking.microscopy.wisc.edu/trac/java/browser/trunk/components/loci-plugins/src/loci/plugins/ColorDialog.java">Trac</a>,
 * <a href="https://skyking.microscopy.wisc.edu/svn/java/trunk/components/loci-plugins/src/loci/plugins/ColorDialog.java">SVN</a></dd></dl>
 *
 * @author Melissa Linkert linkert at wisc.edu
 * @author Curtis Rueden ctrueden at wisc.edu
 */
public class ColorDialog extends ImporterDialog {

  // -- Constants --

  /** Default custom colors for each channel. */
  private static final Color[] DEFAULT_COLORS = {
    Color.red,
    Color.green,
    Color.blue,
    Color.white,
    Color.cyan,
    Color.magenta,
    Color.yellow
  };
  private static final Dimension SWATCH_SIZE = new Dimension(100, 50);

  // -- Fields --

  private List<TextField> colors;

  // -- Constructor --

  public ColorDialog(ImportProcess process) {
    super(process);
  }

  // -- ImporterDialog methods --

  @Override
  protected boolean needPrompt() {
    return !process.isWindowless() && options.isColorModeCustom();
  }

  @Override
  protected GenericDialog constructDialog() {
    GenericDialog gd = new GenericDialog("Bio-Formats Custom Colorization");

    // CTR TODO - avoid problem with MAX_SLIDERS in GenericDialog
    final ImageProcessorReader reader = process.getReader();
    final List<Panel> swatches = new ArrayList<Panel>();
    for (int s=0; s<process.getSeriesCount(); s++) {
      if (!options.isSeriesOn(s)) continue;
      reader.setSeries(s);
      for (int c=0; c<reader.getSizeC(); c++) {
        Color color = options.getCustomColor(s, c);
        if (color == null) color = DEFAULT_COLORS[c % DEFAULT_COLORS.length];
        gd.addSlider(makeLabel("Red:", s, c), 0, 255, color.getRed());
        gd.addSlider(makeLabel("Green:", s, c), 0, 255, color.getGreen());
        gd.addSlider(makeLabel("Blue:", s, c), 0, 255, color.getBlue());

        Panel swatch = createSwatch(color);
        gd.addPanel(swatch, GridBagConstraints.CENTER, new Insets(5, 0, 5, 0));
        swatches.add(swatch);
      }
    }

    // change swatch colors when sliders move
    List<TextField> colorFields = gd.getNumericFields();
    attachListeners(colorFields, swatches);

    WindowTools.addScrollBars(gd);

    return gd;
  }

  @Override
  protected boolean harvestResults(GenericDialog gd) {
    final ImageProcessorReader reader = process.getReader();
    for (int s=0; s<process.getSeriesCount(); s++) {
      if (!options.isSeriesOn(s)) continue;
      reader.setSeries(s);
      for (int c=0; c<reader.getSizeC(); c++) {
        int red = (int) gd.getNextNumber();
        int green = (int) gd.getNextNumber();
        int blue = (int) gd.getNextNumber();
        Color color = new Color(red, green, blue);
        options.setCustomColor(s, c, color);
      }
    }
    return true;
  }

  // -- Helper methods --

  private String makeLabel(String baseLabel, int s, int c) {
    return "Series_" + s + "_Channel_" + c + "_" + baseLabel;
  }

  private Panel createSwatch(Color color) {
    Panel swatch = new Panel();
    swatch.setPreferredSize(SWATCH_SIZE);
    swatch.setMinimumSize(SWATCH_SIZE);
    swatch.setMaximumSize(SWATCH_SIZE);
    swatch.setBackground(color);
    return swatch;
  }

  private void attachListeners(List<TextField> colors, List<Panel> swatches) {
    int colorIndex = 0, swatchIndex = 0;
    while (colorIndex < colors.size()) {
      final TextField redField = colors.get(colorIndex++);
      final TextField greenField = colors.get(colorIndex++);
      final TextField blueField = colors.get(colorIndex++);
      final Panel swatch = swatches.get(swatchIndex++);
      TextListener textListener = new TextListener() {
        public void textValueChanged(TextEvent e) {
          int red = getColorValue(redField);
          int green = getColorValue(greenField);
          int blue = getColorValue(blueField);
          swatch.setBackground(new Color(red, green, blue));
          swatch.repaint();
        }
      };
      redField.addTextListener(textListener);
      greenField.addTextListener(textListener);
      blueField.addTextListener(textListener);
    }
  }

  private int getColorValue(TextField colorField) {
    int color = 0;
    try {
      color = Integer.parseInt(colorField.getText());
    }
    catch (NumberFormatException exc) { }
    if (color < 0) color = 0;
    if (color > 255) color = 255;
    return color;
  }

}