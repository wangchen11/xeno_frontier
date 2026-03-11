package person.wangchen11.gdx;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import person.wangchen11.planet.game.PlanetGame;

public class DesktopLauncher {
	public static void main(String[] arg) {
		boolean terrainTransitionTest = false;
		boolean terrainTransitionMaskTest = false;
		for (String value : arg) {
			if ("--terrain-transition-test".equals(value)) {
				terrainTransitionTest = true;
			}
			if ("--terrain-transition-mask-test".equals(value)) {
				terrainTransitionMaskTest = true;
			}
		}

		Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
		boolean useTerrainTestWindow = terrainTransitionTest || terrainTransitionMaskTest;
		config.setWindowedMode(useTerrainTestWindow ? 1400 : 1280, useTerrainTestWindow ? 900 : 720);
		config.setForegroundFPS(60);
		config.setTitle("寂宿耕境");

		new Lwjgl3Application(
			new PlanetGame(
				terrainTransitionMaskTest
					? PlanetGame.LaunchMode.TERRAIN_TRANSITION_MASK_TEST
					: terrainTransitionTest
					? PlanetGame.LaunchMode.TERRAIN_TRANSITION_TEST
					: PlanetGame.LaunchMode.NORMAL
			),
			config
		);
	}
}
