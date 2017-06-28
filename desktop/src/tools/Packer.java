package tools;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.tools.texturepacker.TexturePacker;
/**
 * With <3 by matej on 20/09/16.
 */


public class Packer {
    public static void main(String[] args) {
        String inputDir = "./assets/images/";
        String outputDir = "./android/assets/images/letters/";
        String packFileName = "letters";
        TexturePacker.process(inputDir, outputDir, packFileName);
    }
}
