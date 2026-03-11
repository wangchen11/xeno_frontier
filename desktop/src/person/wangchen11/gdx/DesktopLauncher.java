package person.wangchen11.gdx;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import person.wangchen11.planet.game.PlanetGame;

public class DesktopLauncher {
	public static void main(String[] arg) {
		boolean terrainTransitionTest = false;
		for (String value : arg) {
			if ("--terrain-transition-test".equals(value)) {
				terrainTransitionTest = true;
				break;
			}
		}

		Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
		config.setWindowedMode(terrainTransitionTest ? 1400 : 1280, terrainTransitionTest ? 900 : 720);
		config.setForegroundFPS(60);
		config.setTitle("寂宿耕境");

		new Lwjgl3Application(
			new PlanetGame(
				terrainTransitionTest
					? PlanetGame.LaunchMode.TERRAIN_TRANSITION_TEST
					: PlanetGame.LaunchMode.NORMAL
			),
			config
		);
	}
}
