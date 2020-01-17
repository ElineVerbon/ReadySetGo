package tests;

import gui.*;

public class GuiTest {
	public GuiTest() {
		GoGuiIntegrator g = new GoGuiIntegrator(true, true, 10);
		g.startGUI();
		g.setBoardSize(10);
	}
	public static void main(String[] args) {
		new GuiTest();
	}
}
