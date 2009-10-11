/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ch.unizh.ini.jaer.projects.cochsoundloc;

import ch.unizh.ini.jaer.chip.cochlea.BinauralCochleaEvent;
import ch.unizh.ini.jaer.chip.cochlea.BinauralCochleaEvent.Ear;
import ch.unizh.ini.jaer.chip.cochlea.CochleaAMSEvent;
import java.awt.*;
import java.awt.Dimension;
import java.util.Arrays;
import java.util.Observable;
import java.util.Observer;
import javax.swing.JFrame;
import net.sf.jaer.chip.AEChip;
import net.sf.jaer.event.BasicEvent;
import net.sf.jaer.event.EventPacket;
import net.sf.jaer.eventprocessing.EventFilter2D;
import net.sf.jaer.util.chart.Axis;
import net.sf.jaer.util.chart.Category;
import net.sf.jaer.util.chart.Series;
import net.sf.jaer.util.chart.XYChart;

/**
 *
 * @author Holger (some parts are from tobi's ISIhistogrammer)
 *
 */
public class ISIFilter extends EventFilter2D implements Observer {

    private int nBins = getPrefs().getInt("ISIFilter.nBins", 50);
    private int maxIsiUs = getPrefs().getInt("ISIFilter.maxIsiUs", 10000);
    private int minIsiUs = getPrefs().getInt("ISIFilter.minIsiUs", 0);
    private int neighborhoodLower = getPrefs().getInt("ISIFilter.neighborhoodLower", 0);
    private int neighborhoodUpper = getPrefs().getInt("ISIFilter.neighborhoodUpper", 0);
    private boolean useLeftEar = getPrefs().getBoolean("ISIFilter.useLeftEar", true);
    private boolean useRightEar = getPrefs().getBoolean("ISIFilter.useRightEar", true);
    private boolean includeSameChannel = getPrefs().getBoolean("ISIFilter.includeSameChannel", true);
    private boolean includeSameNeuron = getPrefs().getBoolean("ISIFilter.includeSameNeuron", true);
    private boolean includeOtherNeurons = getPrefs().getBoolean("ISIFilter.includeOtherNeurons", false);
    private String useChannels = getPrefs().get("ISIFilter.useChannels", "1-64");
    private boolean[] useChannelsBool = new boolean[64];
    private float[] bins = new float[nBins];
    private int maxBinIndex = 0;
    int nChans = 1;
    int[][][] lastTs = new int[64][4][2];
    JFrame isiFrame = null;
    int nextDecayTimestamp = 0, lastDecayTimestamp = 0;
    private int tauDecayMs = getPrefs().getInt("ISIFilter.tauDecayMs", 1000);

    public ISIFilter(AEChip chip) {
        super(chip);
        chip.addObserver(this);
        setPropertyTooltip("maxIsiUs", "maximim ISI in us, larger ISI's are discarded");
        setPropertyTooltip("minIsiUs", "minimum ISI in us, smaller ISI's are discarded");
        setPropertyTooltip("NBins", "number of histogram bins");
        setPropertyTooltip("isiBetween", "Between which channels the ISI is to be computed");
        setPropertyTooltip("tauDecayMs", "histogram bins are decayed to zero with this time constant in ms");
        setPropertyTooltip("logPlotEnabled", "enable to plot log histogram counts, disable for linear scale");
        setPropertyTooltip("useChannels", "channels to use for the histogram seperated by ; (i.e. '1-5;10-15;20-25')");
        setPropertyTooltip("useLeftEar", "Use the left ear");
        setPropertyTooltip("useRightEar", "Use the right ear");
        setPropertyTooltip("neighborhoodLower", "The number of lower neighboring channels to use for ISIs");
        setPropertyTooltip("neighborhoodUpper", "The number of upper neighboring channels to use for ISIs");
        setPropertyTooltip("includeSameChannel", "If ISIs should be computed to the last spike in the same channel");
        setPropertyTooltip("includeSameNeuron", "If ISIs should be computed to the last spike in the same neuron");
        setPropertyTooltip("includeOtherNeurons", "If ISIs should be computed to the last spikes in other neurons");
        parseUseChannel();
        resetBins();
    }

    @Override
    synchronized public EventPacket<?> filterPacket(EventPacket<?> in) {
        for (BasicEvent e : in) {
            try {
                BinauralCochleaEvent i = (BinauralCochleaEvent) e;
                int ear;
                if (i.getEar() == Ear.LEFT) {
                    ear = 0;
                    if (useLeftEar == false) {
                        break;
                    }
                } else {
                    ear = 1;
                    if (useRightEar == false) {
                        break;
                    }
                }
                CochleaAMSEvent camsevent = ((CochleaAMSEvent) e);
                int neuron = camsevent.getThreshold();
                int ts = e.timestamp;
                int ch = e.x;
                if (!useChannelsBool[ch]) {
                    continue;
                }
                int start = ch - this.neighborhoodLower;
                if (start < 0) {
                    start = 0;
                }
                for (int compareChan = start; compareChan <= ch + this.neighborhoodUpper && compareChan < 64; compareChan++) {
                    if (this.includeSameChannel || ch != compareChan) {
                        for (int compareNeuron = 1; compareNeuron < 4; compareNeuron++) {
                            if (neuron == compareNeuron && ch == compareChan) {
                                if (this.includeSameNeuron) {
                                    addIsi(ts - lastTs[ch][compareNeuron][ear]);
                                }
                            } else {
                                if (this.includeOtherNeurons) {
                                    addIsi(ts - lastTs[ch][compareNeuron][ear]);
                                }
                            }
                        }
                    }
                }

                lastTs[ch][neuron][ear] = ts;
                decayHistogram(ts);
            } catch (Exception e1) {
                log.warning("In for-loop in filterPacket caught exception " + e1);
                e1.printStackTrace();
            }
        }
        isiFrame.repaint();
        return in;
    }

    synchronized public void resetBins() {

        if (bins.length != nBins) {
            bins = new float[nBins];
        }
        Arrays.fill(bins, 0);
        maxBinIndex = 0;
        if (activitySeries != null) {
            activitySeries.setCapacity(nBins);
        }
        if (isiFrame != null) {
            isiFrame.repaint(0);
        }
    }

    private void addIsi(int isi) {
        if (isi < minIsiUs) {
            return;
        }
        if (isi >= maxIsiUs) {
            return;
        }

        int bin = (((isi - minIsiUs) * nBins) / (maxIsiUs - minIsiUs));

        bins[bin]++;

        if (bins[bin] > bins[maxBinIndex]) {
            maxBinIndex = bin;
        }
    }

    public void doPrintBins() {
        for (float i : bins) {
            System.out.print(String.format("%.0f ", i));
        }
        System.out.println("");
    }

    @Override
    public Object getFilterState() {
        return null;
    }

    @Override
    public void resetFilter() {
        resetBins();
    }

    @Override
    public void initFilter() {
        resetBins();
    }

    public void update(Observable o, Object arg) {
        if (arg.equals("sizeX") || arg.equals("sizeY")) {
            resetBins();
        }
    }

    /**
     * @return the nBins
     */
    public int getNBins() {
        return nBins;
    }

    /**
     * @param nBins the nBins to set
     */
    public void setNBins(int nBins) {
        int old = this.nBins;
        if (nBins < 1) {
            nBins = 1;
        }
        this.nBins = nBins;
        getPrefs().putInt("ISIFilter.nBins", nBins);
        resetBins();
        support.firePropertyChange("nBins", old, this.nBins);
    }

    /**
     * @return the neighborhoodLower
     */
    public int getNeighborhoodLower() {
        return neighborhoodLower;
    }

    /**
     * @param neighborhoodLower the neighborhoodLower to set
     */
    public void setNeighborhoodLower(int neighborhoodLower) {
        int oldneighborhoodLower = this.neighborhoodLower;
        this.neighborhoodLower = neighborhoodLower;
        getPrefs().putFloat("ISIFilter.neighborhoodLower", neighborhoodLower);
        support.firePropertyChange("neighborhoodLower", oldneighborhoodLower, this.neighborhoodLower);
    }

    /**
     * @return the neighborhoodUpper
     */
    public int getNeighborhoodUpper() {
        return neighborhoodUpper;
    }

    /**
     * @param neighborhoodUpper the neighborhoodUpper to set
     */
    public void setNeighborhoodUpper(int neighborhoodUpper) {
        int oldneighborhoodUpper = this.neighborhoodUpper;
        this.neighborhoodUpper = neighborhoodUpper;
        getPrefs().putFloat("ISIFilter.neighborhoodUpper", neighborhoodUpper);
        support.firePropertyChange("neighborhoodUpper", oldneighborhoodUpper, this.neighborhoodUpper);
    }

    /**
     * @return the maxIsiUs
     */
    public int getMaxIsiUs() {
        return maxIsiUs;
    }

    /**
     * @param maxIsiUs the maxIsiUs to set
     */
    synchronized public void setMaxIsiUs(int maxIsiUs) {
        int old = this.maxIsiUs;
        if (maxIsiUs < minIsiUs) {
            maxIsiUs = minIsiUs;
        }
        this.maxIsiUs = maxIsiUs;
        getPrefs().putInt("ISIFilter.maxIsiUs", maxIsiUs);
        resetBins();
        if (binAxis != null) {
            binAxis.setUnit(String.format("%d,%d us", minIsiUs, maxIsiUs));
        }
        if (isiFrame != null) {
            isiFrame.repaint();
        }
        support.firePropertyChange("maxIsiUs", old, maxIsiUs);
    }

    /**
     * @return the minIsiUs
     */
    public int getMinIsiUs() {
        return minIsiUs;
    }

    /**
     * @param minIsiUs the minIsiUs to set
     */
    public void setMinIsiUs(int minIsiUs) {
        int old = this.minIsiUs;
        if (minIsiUs > maxIsiUs) {
            minIsiUs = maxIsiUs;
        }
        this.minIsiUs = minIsiUs;
        getPrefs().putInt("ISIFilter.minIsiUs", minIsiUs);
        resetBins();
        if (binAxis != null) {
            binAxis.setUnit(String.format("%d,%d us", minIsiUs, maxIsiUs));
        }
        if (isiFrame != null) {
            isiFrame.repaint();
        }
        support.firePropertyChange("minIsiUs", old, minIsiUs);
    }

    /**
     * @return the tauDecayMs
     */
    public int getTauDecayMs() {
        return tauDecayMs;
    }

    /**
     * @param tauDecayMs the tauDecayMs to set
     */
    public void setTauDecayMs(int tauDecayMs) {
        int oldtau = this.tauDecayMs;
        this.tauDecayMs = tauDecayMs;
        getPrefs().putFloat("ISIFilter.tauDecayMs", tauDecayMs);
        support.firePropertyChange("tauDecayMs", oldtau, this.tauDecayMs);
    }

    @Override
    public synchronized void setFilterEnabled(boolean yes) {
        super.setFilterEnabled(yes);
        setIsiDisplay(yes);
    }

    @Override
    public synchronized void cleanup() {
        super.cleanup();
        setIsiDisplay(false);
    }

    public void decayHistogram(int timestamp) {
        if (timestamp > lastDecayTimestamp) {
            float decayconstant = (float) java.lang.Math.exp(-(float) (timestamp - lastDecayTimestamp) / (float) (tauDecayMs * 1000));
            for (int i = 0; i < bins.length; i++) {
                bins[i] = bins[i] * decayconstant;
            }
        }
        lastDecayTimestamp = timestamp;
    }
    public Series activitySeries;
    private Axis binAxis;
    private Axis activityAxis;
    private Category activityCategory;
    private XYChart chart;
    private boolean logPlotEnabled = getPrefs().getBoolean("ISIFilter.logPlotEnabled", false);

    private void setIsiDisplay(boolean yes) {
        if (!yes) {
            if (isiFrame != null) {
                isiFrame.dispose();
                isiFrame = null;
            }
        } else {
            isiFrame = new JFrame("ISIs") {

                @Override
                synchronized public void paint(Graphics g) {
                    super.paint(g);
                    try {
                        if (bins != null) {
                            activitySeries.clear();

                            //log.info("numbins="+myBins.numOfBins);
                            for (int i = 0; i < nBins; i++) {
                                if (isLogPlotEnabled()) {
                                    activitySeries.add(i, (float) Math.log(bins[i]));
                                } else {
                                    activitySeries.add(i, bins[i]);
                                }
                            }

                            binAxis.setMaximum(nBins);
                            binAxis.setMinimum(0);
                            if (isLogPlotEnabled()) {
                                activityAxis.setMaximum((float) Math.log(bins[maxBinIndex]));
                            } else {
                                activityAxis.setMaximum(bins[maxBinIndex]);
                            }
                        } else {
                            log.warning("bins==null");
                        }
                    } catch (Exception e) {
                        log.warning("while displaying bins chart caught " + e);
                    }
                }
            };
            isiFrame.setPreferredSize(new Dimension(200, 100));
            Container pane = isiFrame.getContentPane();
            chart = new XYChart();
            activitySeries = new Series(2, nBins);

            binAxis = new Axis(0, nBins);
            binAxis.setTitle("bin");
            binAxis.setUnit(String.format("%d,%d us", minIsiUs, maxIsiUs));

            activityAxis = new Axis(0, 1); // will be normalized
            activityAxis.setTitle("count");

            activityCategory = new Category(activitySeries, new Axis[]{binAxis, activityAxis});
            activityCategory.setColor(new float[]{1f, 1f, 1f}); // white for visibility
            activityCategory.setLineWidth(3f);

            chart = new XYChart("ISIs");
            chart.setBackground(Color.black);
            chart.setForeground(Color.white);
            chart.setGridEnabled(false);
            chart.addCategory(activityCategory);

            pane.setLayout(new BorderLayout());
            pane.add(chart, BorderLayout.CENTER);

            isiFrame.setVisible(yes);
        }

    }

    /**
     * @return the logPlotEnabled
     */
    public boolean isLogPlotEnabled() {
        return logPlotEnabled;
    }

    /**
     * @param logPlotEnabled the logPlotEnabled to set
     */
    public void setLogPlotEnabled(boolean logPlotEnabled) {
        this.logPlotEnabled = logPlotEnabled;
        getPrefs().putBoolean("ISIFilter.logPlotEnabled", logPlotEnabled);
    }

    public boolean isUseLeftEar() {
        return this.useLeftEar;
    }

    public void setUseLeftEar(boolean useLeftEar) {
        getPrefs().putBoolean("ISIFilter.useLeftEar", useLeftEar);
        this.useLeftEar = useLeftEar;
    }

    public boolean isUseRightEar() {
        return this.useRightEar;
    }

    public void setUseRightEar(boolean useRightEar) {
        getPrefs().putBoolean("ISIFilter.useRightEar", useRightEar);
        this.useRightEar = useRightEar;
    }

    public boolean getIncludeSameChannel() {
        return this.includeSameChannel;
    }

    public void setIncludeSameChannel(boolean includeSameChannel) {
        getPrefs().putBoolean("ISIFilter.includeSameChannel", includeSameChannel);
        this.includeSameChannel = includeSameChannel;
    }

    public boolean getIncludeSameNeuron() {
        return this.includeSameNeuron;
    }

    public void setIncludeSameNeuron(boolean includeSameNeuron) {
        getPrefs().putBoolean("ISIFilter.includeSameNeuron", includeSameNeuron);
        this.includeSameNeuron = includeSameNeuron;
    }

    public boolean getIncludeOtherNeurons() {
        return this.includeOtherNeurons;
    }

    public void setIncludeOtherNeurons(boolean includeOtherNeurons) {
        getPrefs().putBoolean("ISIFilter.includeOtherNeurons", includeOtherNeurons);
        this.includeOtherNeurons = includeOtherNeurons;
    }

    /**
     * @return the useChannels
     */
    public String getUseChannels() {
        return this.useChannels;
    }

    /**
     * @param useChannels the channels to use
     */
    public void setUseChannels(String useChannels) {
        support.firePropertyChange("useChannels", this.useChannels, useChannels);
        this.useChannels = useChannels;
        getPrefs().put("ISIFilter.useChannels", useChannels);
        parseUseChannel();
    }

    private void parseUseChannel() {
        for (int i = 0; i < 64; i++) {
            useChannelsBool[i] = false;
        }
        String[] temp = useChannels.split(";");
        for (int i = 0; i < temp.length; i++) {
            String[] temp2 = temp[i].split("-");
            if (temp2.length == 1) {
                useChannelsBool[Integer.parseInt(temp2[0])] = true;
            } else if (temp2.length == 2) {
                for (int j = Integer.parseInt(temp2[0]) - 1; j < Integer.parseInt(temp2[1]); j++) {
                    useChannelsBool[j] = true;
                }
            }
        }
    }
}
