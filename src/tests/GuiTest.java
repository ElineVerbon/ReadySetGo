package tests;

import com.nedap.go.gui.GoGUIIntegrator;

public class GuiTest {
	public GuiTest() {
		GoGUIIntegrator g = new GoGUIIntegrator(true, true, 10);
		g.startGUI();
		g.setBoardSize(10);
	}
	
	public static void main (String[] args) {
		new GuiTest();
	}
}
