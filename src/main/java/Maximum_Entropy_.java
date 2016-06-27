
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.plugin.filter.PlugInFilter;
import ij.process.ByteProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import java.awt.Rectangle;

/**
 * This is a startup project for imageJ plug-in. It is based on NetBeans IDE
 * 8.1. It uses Apache Maven to build. Make sure the internet connection is
 * working while build the project.
 *
 * @author Luke Chang, 25-02-2016
 *
 */
public class Maximum_Entropy_ implements PlugInFilter{

    double[] entropy;
    // from input dialog
    private Boolean isShowHistogram = false;

    @Override
    public int setup(String string, ImagePlus ip){
        IJ.hideProcessStackDialog = true;
        if(!showDialog()){
            return DONE;
        }
        // add DOES_16, if you want process 16bit greyscale image.
        return DOES_8G + SUPPORTS_MASKING + NO_UNDO;
    }

    private boolean showDialog(){
        // Build dialog;
        GenericDialog gd = new GenericDialog("MyDialog");
        gd.addCheckbox("Show histogram", isShowHistogram);
        // Show dialog;
        gd.showDialog();
        if(gd.wasCanceled()){
            return false;
        }
        // get value
        isShowHistogram = gd.getNextBoolean();
        return true;
    }

    @Override
    public void run(ImageProcessor ip){
        int[] hist = ip.getHistogram();
        entropy = new double[hist.length];
        int threshold = entropySplit(hist);
        ip.threshold(threshold);
        int sum = ip.getPixelCount();
        if(isShowHistogram){
            for(int h : hist){
                System.out.print((Math.round(h / (double)sum * 1000.0) / 1000.0) + ", ");
            }
            System.out.print("\n");
            for(double e : entropy){
                System.out.print((Math.round(e * 1000.0) / 1000.0) + ", ");
            }
            System.out.print("\n");
        }
    }

    private int entropySplit(int[] hist){
        // Normalize histogram, that is makes the sum of all bins equal to 1.
        double sum = 0;
        for(int i = 0; i < hist.length; ++i){
            sum += hist[i];
        }
        if(sum == 0){
            // This should not normally happen, but...
            throw new IllegalArgumentException("Empty histogram: sum of all bins is zero.");
        }
        double[] normalizedHist = new double[hist.length];
        for(int i = 0; i < hist.length; i++){
            normalizedHist[i] = hist[i] / sum;
        }
        //
        double[] pT = new double[hist.length];
        pT[0] = normalizedHist[0];
        for(int i = 1; i < hist.length; i++){
            pT[i] = pT[i - 1] + normalizedHist[i];
        }

        // Entropy for black and white parts of the histogram
        final double epsilon = Double.MIN_VALUE;
        double[] hB = new double[hist.length];
        double[] hW = new double[hist.length];
        for(int t = 0; t < hist.length; t++){
            // Black entropy
            if(pT[t] > epsilon){
                double hhB = 0;
                for(int i = 0; i <= t; i++){
                    if(normalizedHist[i] > epsilon){
                        hhB -= normalizedHist[i] / pT[t] * Math.log(normalizedHist[i] / pT[t]);
                    }
                }
                hB[t] = hhB;
            } else{
                hB[t] = 0;
            }
            // White  entropy
            double pTW = 1 - pT[t];
            if(pTW > epsilon){
                double hhW = 0;
                for(int i = t + 1; i < hist.length; ++i){
                    if(normalizedHist[i] > epsilon){
                        hhW -= normalizedHist[i] / pTW * Math.log(normalizedHist[i] / pTW);
                    }
                }
                hW[t] = hhW;
            } else{
                hW[t] = 0;
            }
        }
        // Find histogram index with maximum entropy
        double jMax = hB[0] + hW[0];
        int tMax = 0;
        for(int t = 1; t < hist.length; ++t){
            double j = hB[t] + hW[t];
            entropy[t] = j;
            if(j > jMax){
                jMax = j;
                tMax = t;
            }
        }
        return tMax;
    }

    /**
     * Main method for IDE to run. Not required for imageJ to compile. Do not
     * alter.
     *
     * @param args
     */
    public static void main(String[] args){
        // set the plugins.dir property to make the plugin appear in the Plugins menu
        Class<?> clazz = Maximum_Entropy_.class;
        String url = clazz.getResource("/" + clazz.getName().replace('.', '/') + ".class").toString();
        String pluginsDir = url.substring(5, url.length() - clazz.getName().length() - 6);
        System.setProperty("plugins.dir", pluginsDir);

        // start ImageJ
        new ImageJ();
    }
}
