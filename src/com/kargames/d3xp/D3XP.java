package com.kargames.d3xp;

import java.awt.AWTException;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class D3XP {
    // The dimensions of the xp bar on the screen, in pixels
    // NOTE: will need to be adjusted for resolutions other than 1920x1080
    static final int XP_BAR_X = 625;
    static final int XP_BAR_Y = 980;

    static final int XP_BAR_WIDTH = 670;
    static final int XP_BAR_HEIGHT = 20;

    static final Rectangle XP_BAR = new Rectangle(XP_BAR_X, XP_BAR_Y,
	    XP_BAR_WIDTH, XP_BAR_HEIGHT);

    // The location of the paragon level on the screen, in pixels
    static final Rectangle PARAGON_LEVEL = new Rectangle(46, 135, 35, 13);

    // in debug mode, show images of the xp bar and paragon level processing
    static final boolean DEBUG = false;

    // The "hash" for digits 0 through 9 for character recognition of the
    // paragon level. Probably only works for 1920x1080
    static final int[] NUMBER_HASHES = { 2250, 617, 1905, 2400, 2354, 2105,
	    2256, 814, 2864, 2600 };

    boolean paused = false;

    JLabel xpBarLeftLabel = new JLabel();
    JLabel xpBarRightLabel = new JLabel();
    JLabel levelImageLabel = new JLabel();
    JFrame frame = new JFrame("Diablo 3 XP Tracker 0.1");
    JLabel avg5MinXpLabel = new JLabel();
    JLabel avgCurrentXpLabel = new JLabel();
    JLabel timeToLevelLabel = new JLabel();
    JLabel timeLabel = new JLabel();
    JLabel levelLabel = new JLabel();
    JLabel currentXpLabel = new JLabel();
    JLabel averageXpLabel = new JLabel();
    ArrayList<Long> xps = new ArrayList<Long>();
    Robot r;
    long startXp = 0;
    DateFormat df = DateFormat.getTimeInstance(DateFormat.SHORT);
    long start = 0;
    int prevLevel = 0;
    int labelNum = 0;

    public D3XP() {
	start = System.currentTimeMillis();

	setupFrame();

	try {
	    r = new Robot();
	    long time = System.currentTimeMillis();
	    while (true) {
		long delta = System.currentTimeMillis() - time;
		time = System.currentTimeMillis();

		if (paused) {
		    start += delta;
		}

		long sec = (System.currentTimeMillis() - start) / 1000;
		timeLabel.setText(String.format("%d:%02d:%02d", sec / 3600,
			(sec % 3600) / 60, (sec % 60)));

		if (sec % 30 < 1 && !paused) {
		    update();
		}
		r.delay(1000);

	    }

	} catch (AWTException e) {
	    e.printStackTrace();
	}
    }

    private void setupFrame() {
	frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	Font font = new Font("Dialog", Font.PLAIN, 36);

	JPanel top = new JPanel(new GridBagLayout());
	JPanel center = new JPanel(new GridLayout(0, 1));
	JPanel panel = new JPanel(new BorderLayout());
	JPanel pauseReset = new JPanel(new GridBagLayout());

	final JButton pauseButton = new JButton("pause");
	pauseButton.addActionListener(new ActionListener() {
	    @Override
	    public void actionPerformed(ActionEvent arg0) {
		paused = !paused;
		if (paused) {
		    pauseButton.setText("resume");
		} else {
		    pauseButton.setText("pause");
		}
	    }
	});
	final JButton resetButton = new JButton("reset");
	resetButton.addActionListener(new ActionListener() {
	    @Override
	    public void actionPerformed(ActionEvent arg0) {
		start = System.currentTimeMillis();
		xps.clear();
		xps = new ArrayList<Long>();
		timeLabel.setText("0:00:00");
	    }
	});

	pauseButton.setPreferredSize(new Dimension(100, 40));
	resetButton.setPreferredSize(new Dimension(100, 40));
	pauseReset.add(pauseButton);
	pauseReset.add(new JLabel("    "));
	pauseReset.add(resetButton);
	center.add(pauseReset);

	addLabels(top, "level:  ", font, levelLabel);
	addLabels(top, "current xp:  ", font, currentXpLabel);
	addLabels(top, "avg (session):  ", font, averageXpLabel);
	addLabels(top, "avg (30 sec):  ", font, avgCurrentXpLabel);
	addLabels(top, "avg (5 min):  ", font, avg5MinXpLabel);
	addLabels(top, "time to level:  ", font, timeToLevelLabel);
	addLabels(top, "time:  ", font, timeLabel);

	if (DEBUG) {
	    center.add(xpBarLeftLabel);
	    center.add(xpBarRightLabel);
	    center.add(levelImageLabel);
	}

	panel.add(top, BorderLayout.NORTH);
	panel.add(center, BorderLayout.CENTER);

	frame.add(panel);
	frame.setVisible(true);
    }

    public void update() {
	double xpPercent = getCurrentXp(r.createScreenCapture(XP_BAR));
	int currentLevel = getCurrentLevel(r.createScreenCapture(PARAGON_LEVEL));

	// if the paragon level text is not visible, the current level will
	// be 0. Use the previously known level
	if (currentLevel == 0) {
	    currentLevel = prevLevel;
	}

	prevLevel = currentLevel;

	// don't attempt to process level 0. This happens if the level has
	// never been visible or if the character is actually paragon 0.
	if (currentLevel > 0) {
	    long xpReq = Experience.getLevelXp(currentLevel);
	    long totalXp = Experience.getTotalXp(currentLevel - 1)
		    + (long) (xpPercent * xpReq);

	    if (xps.size() > 0) {
		long prevXp = xps.get(xps.size() - 1);

		// if the previous xp is more than current, it's probably because
		// the bar was covered by something. Use the previous known
		// xp in this case
		if (prevXp > totalXp) {
		    totalXp = prevXp;
		}
	    }

	    frame.repaint();

	    xps.add(new Long(totalXp));

	    currentXpLabel.setText((int) ((totalXp - Experience.getTotalXp(currentLevel - 1)) / 1000000)
		    + "M / " + (xpReq / 1000000) + "M");  

	    levelLabel.setText((int)currentLevel + " (" + String.format("%.2f", (100 * xpPercent)) + "%) ");

	    if (xps.size() > 10) {
		int avg = (int) ((totalXp - xps.get(xps.size() - 11)) * 12 / 1000000);
		avg5MinXpLabel.setText(avg + " M/hr");
	    } else {
		avg5MinXpLabel.setText("N/A");
	    }

	    if (xps.size() > 1) {
		int avg2 = (int) ((totalXp - xps.get(xps.size() - 2)) * 120 / 1000000);
		avgCurrentXpLabel.setText(avg2 + " M/hr");

		int secs = (int) (System.currentTimeMillis() - start) / 1000;

		long avg = (long) ((totalXp - xps.get(0)) * 3600 / (double) secs);
		averageXpLabel.setText(avg / 1000000l + " M/hr");

		long req = xpReq - (long) (xpPercent * xpReq);
		double ttlHrs = req / (double) avg;
		if (!Double.isInfinite(ttlHrs)) {
		    long t = System.currentTimeMillis() + (long) (ttlHrs * 3600000);
		    Date d = new Date(t);
		    long sec = (long) (ttlHrs * 3600d);
		    timeToLevelLabel.setText(String.format("%d:%02d:%02d",
			    sec / 3600, (sec % 3600) / 60, (sec % 60)) + " ("
			    + df.format(d) + ")");
		} else {
		    timeToLevelLabel.setText("N/A");
		}
	    } else {
		averageXpLabel.setText("N/A");
		timeToLevelLabel.setText("N/A");
		avgCurrentXpLabel.setText("N/A");
	    }
	} else {
	    avgCurrentXpLabel.setText("N/A");
	}
	r.delay(100);
	frame.pack();
    }

    public static void main(String[] args) {
	new D3XP();
    }

    private void addLabels(JPanel top, String string, Font font, JLabel other) {

	GridBagConstraints c = new GridBagConstraints();
	JLabel temp = new JLabel(string);
	temp.setFont(font);
	other.setFont(font);
	c.gridx = 0;
	c.gridy = labelNum;
	c.anchor = GridBagConstraints.EAST;
	top.add(temp, c);
	
	c.gridx = 1;
	c.gridy = labelNum;
	c.anchor = GridBagConstraints.WEST;
	top.add(other,c );
	labelNum++;
    }

    private int getCurrentLevel(BufferedImage image) {
	int w = image.getWidth();
	int h = image.getHeight();
	int sum = 0;
	int width = 0;
	int hash = 0;
	int level = 0;
	for (int x = 0; x < w; x++) {
	    int num = 0;
	    for (int y = 0; y < h; y++) {
		int color = image.getRGB(x, y);
		int blue = color & 0xff;
		if (blue > 160) {
		    image.setRGB(x, y, 0xffffff);
		    num++;
		    hash += y * (width + 1) * (width + 1);
		} else {
		    image.setRGB(x, y, 0x000000);
		}
	    }
	    sum += num;
	    if (num > 0) {
		width++;
	    }
	    if (num == 0 && sum > 0) {
		level *= 10;
		for (int i = 0; i < 10; i++) {

		    if (NUMBER_HASHES[i] == hash) {
			level += i;
		    }
		}
		sum = 0;
		width = 0;
		hash = 0;
	    }
	}
	Image left = image.getSubimage(0, 0, w, h).getScaledInstance(
		(int) (w * 10), (int) (h * 10), BufferedImage.SCALE_DEFAULT);
	levelImageLabel.setIcon(new ImageIcon(left));
	return level;
    }

    private double getCurrentXp(BufferedImage image) {
	int xpEnd = 0;
	int outs = 0;

	for (int i = 0; i < XP_BAR_WIDTH; i++) {
	    int color = image.getRGB(i, 10);
	    int red = (color & 0xff0000) >> 16;
	    int green = (color & 0xff00) >> 8;
	    int blue = color & 0xff;

	    if (red > 20 && red < 70 && green > 30 && green < 95 && blue > 80
		    && blue < 190) {
		image.setRGB(i, 10, 0xffffff);
		outs--;

		if (outs < 100) {
		    xpEnd = i;
		}
	    } else if (red > 10 && red < 45 && green > 20 && green < 60
		    && blue > 25 && blue < 70) {
		image.setRGB(i, 12, 0x00ff00);
		outs += 5;
	    } else if (red < 30 && green < 30 && blue < 30) {
		image.setRGB(i, 10, 0xff0000);
	    } else {
		image.setRGB(i, 10, 0xff0000);
		outs += 5;
	    }
	}

	// draw a white line to indicate where the program thinks the xp
	// bar is
	image.setRGB(xpEnd, 5, 0xffffff);
	image.setRGB(xpEnd, 6, 0xffffff);
	image.setRGB(xpEnd, 7, 0xffffff);
	image.setRGB(xpEnd, 8, 0xffffff);
	image.setRGB(xpEnd, 9, 0xffffff);
	Image left = image.getSubimage(0, 0, XP_BAR_WIDTH / 2, XP_BAR_HEIGHT)
		.getScaledInstance((int) (XP_BAR_WIDTH * 1.5),
			(int) (XP_BAR_HEIGHT * 1.5),
			BufferedImage.SCALE_DEFAULT);
	Image right = image.getSubimage(XP_BAR_WIDTH / 2, 0, XP_BAR_WIDTH / 2,
		XP_BAR_HEIGHT).getScaledInstance((int) (XP_BAR_WIDTH * 1.5),
		(int) (XP_BAR_HEIGHT * 1.5), BufferedImage.SCALE_DEFAULT);
	xpBarLeftLabel.setIcon(new ImageIcon(left));
	xpBarRightLabel.setIcon(new ImageIcon(right));

	return (xpEnd) / (float) (XP_BAR_WIDTH - 3);
    }

}
