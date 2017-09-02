package com.proj.civ.display;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.event.KeyEvent;
import java.util.List;
import java.util.Set;

import com.proj.civ.datastruct.Layout;
import com.proj.civ.datastruct.Point;
import com.proj.civ.datastruct.hex.Hex;
import com.proj.civ.datastruct.hex.HexCoordinate;
import com.proj.civ.datastruct.hex.PathHex;
import com.proj.civ.display.menu.UnitActionMenu;
import com.proj.civ.input.KeyboardHandler;
import com.proj.civ.input.MouseHandler;
import com.proj.civ.instance.IData;
import com.proj.civ.map.civilization.BaseCivilization;
import com.proj.civ.map.terrain.Feature;
import com.proj.civ.map.terrain.YieldType;
import com.proj.civ.unit.Unit;

public class GUI extends IData {
	private int focusX = 0, focusY = 0;
	
	private int scrollX, scrollY, scroll;
	
	private List<PathHex> pathToFollow;
	
	private final Layout layout;
	private final Polygon poly;
	
	private Hex focusHex = null;
	
	public GUI() {
		this.scroll = HEX_RADIUS >> 1;
		
		layout = new Layout(Layout.POINTY_TOP, new Point(HEX_RADIUS, HEX_RADIUS), new Point(HEX_RADIUS, HEX_RADIUS));
		poly = new Polygon();
	}
	
	public void drawHexGrid(Graphics2D g) {
		g.setStroke(new BasicStroke(1.0f));
		int bnd = 8;
		for (int dx = -bnd; dx <= bnd; dx++) {
			for (int dy = Math.max(-bnd, -dx - bnd); dy <= Math.min(bnd, -dx + bnd); dy++) {
				int dz = -dx - dy;
				int centreX = (-scrollX) + WIDTH / 2;
				int centreY = (-scrollY) + HEIGHT / 2;
				
				HexCoordinate hexc = layout.pixelToHex(new Point(centreX, centreY));
				Hex h = hexMap.getHex(new HexCoordinate(hexc.q + dx, hexc.r + dy, hexc.s + dz));
				
				if (h != null) {
					Point p1 = layout.getPolygonPositionEstimate(h);
					if ((p1.x + scrollX < -HEX_RADIUS) || (p1.x + scrollX > WIDTH + HEX_RADIUS) || (p1.y + scrollY < -HEX_RADIUS) || (p1.y + scrollY > HEIGHT + HEX_RADIUS)) {
						continue;
					}
					
					Point[] p2 = layout.polygonCorners(h);
					for (int k = 0; k < p2.length; k++) {
						poly.addPoint((int) (p2[k].x) + scrollX, (int) (p2[k].y) + scrollY);
					}			
					g.setColor(h.getLandscape().getColour());
					g.fillPolygon(poly);
					g.setColor(Color.BLACK);
					g.drawPolygon(poly);
					poly.reset();	
				}
			}
		}
	}
	public void drawSelectedHex(Graphics2D g) {
		int mouseX = MouseHandler.movedMX;
		int mouseY = MouseHandler.movedMY;
		
		g.setStroke(new BasicStroke(3.5f));
			
		HexCoordinate s = layout.pixelToHex(new Point(mouseX - scrollX, mouseY - scrollY));
		if (hexMap.getHex(s) != null) {	
			Point[] p = layout.polygonCorners(s);
			for (int k = 0; k < p.length; k++) {
				poly.addPoint((int) (p[k].x) + scrollX, (int) (p[k].y) + scrollY);
			}
			g.setColor(Color.WHITE);
			g.drawPolygon(poly);
			poly.reset();
		}
	}
	public void drawHexInspect(Graphics2D g) {
		if (KeyboardHandler.ShiftPressed) {
			int mouseX = MouseHandler.movedMX;
			int mouseY = MouseHandler.movedMY;
			
			HexCoordinate h = layout.pixelToHex(new Point(mouseX - scrollX, mouseY - scrollY));
			Hex h1 = hexMap.getHex(h);
			
			if (h1 != null) {
				g.setColor(Color.WHITE);
				g.setFont(new Font("SansSerif", Font.BOLD, 16));
				
				FontMetrics m = g.getFontMetrics();
				List<Feature> features = h1.getFeatures();
				
				StringBuilder hexBox = new StringBuilder();
				
				String landscape = null;
				String improvement = null;
				StringBuilder sbUnits = new StringBuilder();
				StringBuilder sbFeatures = new StringBuilder();
				
				Unit[] units = h1.getUnits();
				
				int xOff = g.getFont().getSize();
				int padding = 3;
				int rectArcRatio = 20;
				int yOff = g.getFontMetrics().getHeight();
				
				int rectW = 200;
				int rectH = yOff * 2 + padding;
				
				landscape = "Landscape: " + h1.getLandscape().getName() + "\n";
				
				if (h1.getImprovement() != null) {
					improvement = "Improvement: " + h1.getImprovement().getName() + "\n";
					rectH += yOff;
				}
				
				if (features.size() > 0) {
					sbFeatures.append("Features: \n");
					features.forEach(i -> sbFeatures.append("- " + i.getName() + "\n"));
					rectH += ((features.size() + 1) * yOff);
				}
				
				for (Unit u : units) {
					if (u != null && u.getPosition().isEqual(new HexCoordinate(h1.q, h1.r, h1.s))) {
						sbUnits.append("(" + u.getOwner().getPluralName() + ") " 
									+ u.getName() + " :\n" 
									+ u.getStrength() + " Strength\n" 
									+ u.getMovementPotential() + "/" + u.getTotalMovement() + " Movement\n");	
						rectH += yOff * 3;
					}
				}
				
				
				if (landscape != null) hexBox.append(landscape);
				if (features.size() > 0) hexBox.append(sbFeatures.toString());
				if (improvement != null) hexBox.append(improvement);
				if (units != null) hexBox.append(sbUnits.toString());
				
				
				boolean flip = ((mouseX - rectW < 0) || mouseY - rectH < 0);
				int startX = flip ? mouseX + padding: mouseX - rectW + padding;
				int startY = flip ? mouseY : mouseY - rectH;
				
				//Draw rectangle at the mouse
				g.fillRoundRect(startX - padding, startY, rectW, rectH, rectW / rectArcRatio, rectH / rectArcRatio);
				
				//Write text in the box about hex yeild
				drawYieldAmount(g, YieldType.FOOD, Color.GREEN, h1, m, startX, startY, 0);
				drawYieldAmount(g, YieldType.PRODUCTION, new Color(150, 75, 5), h1, m, startX, startY, xOff);
				drawYieldAmount(g, YieldType.SCIENCE, Color.BLUE, h1, m, startX, startY, xOff * 2);
				drawYieldAmount(g, YieldType.GOLD, new Color(244, 244, 34), h1, m, startX, startY, xOff * 3);
				
				drawStringBuilderData(g, hexBox, startX, startY + yOff, yOff);
			}
		}
	}
	private void drawYieldAmount(Graphics2D g, YieldType yield, Color c, Hex h, FontMetrics m, int x, int y, int xOff) {
		String amount = Integer.toString(h.getYieldTotal(yield));
		int widthX = m.stringWidth(amount);
		g.setColor(c);
		g.drawString(amount, x + widthX + xOff, y + m.getHeight());
	}
	private void drawStringBuilderData(Graphics2D g, StringBuilder s, int x, int y, int yOff) {
		g.setColor(Color.BLACK);
		for (String l : s.toString().split("\n")) {
			g.drawString(l, x, y += yOff);
		}
	}	
	public void drawPath(Graphics2D g) {
		if (focusHex != null) {
			if (pathToFollow != null) {
				for (PathHex h : pathToFollow) {
					if (!h.equals(focusHex)) {
						if (h.getPassable() || h.getCanSwitch()) {
							g.setColor(Color.WHITE);
						} else {
							g.setColor(Color.RED);
						}
						Point hexCentre = layout.hexToPixel(h);
						g.drawOval((int) (hexCentre.x + scrollX) - 10, (int) (hexCentre.y + scrollY) - 10, 20, 20);
					}
				}	
			}
		}
	}
	public void drawFocusHex(Graphics2D g) {
		if (focusHex != null) {
			if (hexMap.getHex(focusHex) != null) {	
				g.setStroke(new BasicStroke(5.0f));
				
				Point[] p = layout.polygonCorners(focusHex);
				for (int k = 0; k < p.length; k++) {
					poly.addPoint((int) (p[k].x) + scrollX, (int) (p[k].y) + scrollY);
				}
				g.setColor(Color.WHITE);
				g.drawPolygon(poly);
				poly.reset();
			}
		}
	}
	public void drawUnits(Graphics2D g) {
		g.setColor(Color.BLACK);
		g.setFont(new Font("SansSerif", Font.BOLD, 16));
		
		for (BaseCivilization c : civs) {
			List<Unit> units = c.getUnits();
			if (units.size() > 0) g = enableAntiAliasing(g);
			for (Unit u : units) {
				Hex h = hexMap.getHex(u.getPosition());
				Point p = layout.hexToPixel(h);
				String name = u.getName().substring(0, 1);
				int textX = (name.length() * g.getFontMetrics().charWidth(name.charAt(0))) >> 1;
				int textY = g.getFontMetrics().getHeight();
				int x = (int) (p.x + scrollX - (HEX_RADIUS >> 2));
				int y = (int) (p.y + scrollY - (HEX_RADIUS >> 2));
				drawUnit(g, u, x, y, HEX_RADIUS >> 1, textX, textY, name);
			}
		}
	}	
	private void drawUnit(Graphics2D g, Unit u, int x, int y, int radius, int textX, int textY, String name) {
		Color baseCol = u.getOwner().getColour();
		Color cB = baseCol.brighter();
		Color cD = baseCol.darker();
	    
		g.setColor(baseCol);
		g.fillOval(x, y, radius, radius);
		
		g.setStroke(new BasicStroke(1.75f));
		g.setColor(cD);
		g.drawArc(x, y, radius, radius, 50, 200);
		
		g.setColor(cB);
		g.drawArc(x, y, radius, radius, 50, -160);
		
		g.setColor(Color.WHITE);
		g.drawString(name.substring(0, 1), (x + radius / 2) - textX, (y + radius / 2) + textY / 4);
	}
	public void drawUI(Graphics2D g) {
		g.setColor(Color.BLACK);
		g.setFont(new Font("SansSerif", Font.BOLD, 16));
		
		int fontHeight = g.getFontMetrics().getHeight();
		int yieldHeight = (int) (0.75 * fontHeight);
		//Draw the top bar of the ui
		g.fillRect(0, 0, WIDTH, fontHeight);
		
		//Get the civ yield per turn
		int civSciencePT = civs.get(0).getSciencePT();
		int civGoldTotal = civs.get(0).getGoldTotal();
		int civGoldPT = civs.get(0).getGoldPT();
		int civCultureTotal = civs.get(0).getCultureTotal();
		int civCulturePT = civs.get(0).getCulturePT();
		int civCultureReq = civs.get(0).getCultureRequired();
		
		
		//Draw the yields
		g.setColor(new Color(91, 154, 255));
		g.drawString("+" + Integer.toString(civSciencePT), 0, yieldHeight); //Science
		
		g.setColor(new Color(244, 244, 34));
		g.drawString(Integer.toString(civGoldTotal) + "(+" + Integer.toString(civGoldPT) + ")", 100, yieldHeight); //Gold
		
		g.setColor(new Color(186, 16, 160));
		g.drawString(Integer.toString(civCultureTotal) + "/" + Integer.toString(civCultureReq) + "(+" + Integer.toString(civCulturePT) + ")", 200, yieldHeight);
		
		//Draw the turn counter
		g.setColor(Color.WHITE);
		g.drawString("Turn: " + turnCounter, WIDTH - 100, yieldHeight);
	}
	public void drawActionMenus(Graphics2D g) {
		menus.stream().filter(i -> i.getIsActive()).forEach(j -> j.draw(g));
	}
	
	private Graphics2D enableAntiAliasing(Graphics2D g) {
		RenderingHints rh = new RenderingHints(
	             RenderingHints.KEY_ANTIALIASING,
	             RenderingHints.VALUE_ANTIALIAS_ON);
	    g.setRenderingHints(rh);
	    return g;
	}

	public void setInitialScroll(HexCoordinate h) {
		Point p = layout.hexToPixel(new Hex(h.q, h.r, h.s));
		int sX = Math.min(((int) -p.x + (WIDTH >> 2)), HEX_RADIUS); //Ensure the units are shown on-screen
		int sY = Math.min(((int) -p.y + (HEIGHT >> 2)), 0);
		
		
		//Round the values to a multiple of the scroll value
		scrollX = sX + scroll / 2;
		scrollX -= scrollX % scroll;
		
		scrollY = sY + scroll / 2;
		scrollY -= scrollY % scroll;
		
		//Ensure the scroll is not outside bounds
		if (scrollX < -getAdjustedWidth()) {
			scrollX = -getAdjustedWidth();
		}
		if (scrollY < -getAdjustedHeight()) {
			scrollY = -getAdjustedHeight();
		}
	}
	
	public void updateKeys(Set<Integer> keys) {
		if (keys.size() > 0) {
			for (Integer k : keys) {
				switch (k) {
				case KeyEvent.VK_UP:
					scrollY += scrollY < 0 ? scroll : 0;						
					break;
				case KeyEvent.VK_DOWN:
					scrollY -= scrollY > -(getAdjustedHeight()) ? scroll : 0;
					break;
				case KeyEvent.VK_LEFT:
					scrollX += scrollX < HEX_RADIUS ? scroll : 0;
					break;
				case KeyEvent.VK_RIGHT:
					scrollX -= scrollX > -(getAdjustedWidth()) ? scroll : 0;
					break;
				case KeyEvent.VK_SHIFT:
					KeyboardHandler.ShiftPressed = true;
					break;
				case KeyEvent.VK_ESCAPE:
					setFocusedUnitPath(null);
					break;
				case KeyEvent.VK_N:
					//Start the next turn
					nextTurn();
					break;
				case KeyEvent.VK_K:
					menus.add(new UnitActionMenu(true));
					break;
				case KeyEvent.VK_L:
					if (menus.size() > 0) {
						menus.remove(0);
					}
					break;
				//case KeyEvent.VK_F:
				//	farmToAdd = true;
				//	break;
				}
			}
			//System.out.println("ScrollX:" + scrollX + ", ScrollY:" + scrollY);
		}
	}
	
	private void nextTurn() { //Temp code
		if (nextTurnInProgress == false) {
			nextTurnInProgress = true;
			for (BaseCivilization c : civs) {
				for (Unit u : c.getUnits()) {
					u.nextTurn();
					turnCounter++;
				}
			}
		}
	}
	
	/*
	public void addFarm() {
		if (farmToAdd) {
			farmToAdd = false;
			int mX = MouseHandler.movedMX;
			int mY = MouseHandler.movedMY;
			FractionalHex fh = Layout.pixelToHex(layout, new Point(mX - scrollX, mY - scrollY));
			Hex h = FractionalHex.hexRound(fh);
			int hexKey = HexMap.hash(h);
			Improvement i = new Farm();
			Hex mapHex = map.get(hexKey);
			mapHex.setImprovement(i);
			map.put(hexKey, mapHex);
		}
	}
	*/
	
	private int getAdjustedWidth() {
		return (int) ((Math.sqrt(3) * HEX_RADIUS * W_HEXES) - WIDTH);
	}
	private int getAdjustedHeight() {
		return (int) ((H_HEXES * HEX_RADIUS * 3 / 2) - HEIGHT + HEX_RADIUS);
	}
	
	/*
	public void registerFonts(String name) {
	    Font font = null;
	        String fName = Params.get().getFontPath() + name;
	        File fontFile = new File(fName);
	        font = Font.createFont(Font.TRUETYPE_FONT, fontFile);
	        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();

	        ge.registerFont(font);
	}
	*/
	
	public Point getHexPosFromMouse() {
		return new Point(MouseHandler.mX - scrollX, MouseHandler.mY - scrollY);
	}
	public int getScrollX() {
		return scrollX;
	}
	public int getScrollY() {
		return scrollY;
	}
	public void setFocusHex() {
		if (MouseHandler.pressedMouse && (focusHex == null)) {
			focusX = MouseHandler.mX;
			focusY = MouseHandler.mY;
			HexCoordinate tempFocusHex = layout.pixelToHex(new Point(focusX - scrollX, focusY - scrollY));
			Hex mapHex = hexMap.getHex(tempFocusHex);
			if ((!mapHex.canSetMilitary() || !mapHex.canSetCivilian())) {
				focusHex = new Hex(tempFocusHex.q, tempFocusHex.r, tempFocusHex.s);		
			}
		}
	}
	public void resetFocusData() {
		this.focusHex = null;
		currentUnit = null;
	}
	public Hex getFocusHex() {
		return this.focusHex;
	}
	public void setFocusedUnitPath(List<PathHex> pathToFollow) {
		if (pathToFollow == null) resetFocusData();
		this.pathToFollow = pathToFollow;
	}
	public List<PathHex> getUnitPath() {
		return pathToFollow;
	}
}
