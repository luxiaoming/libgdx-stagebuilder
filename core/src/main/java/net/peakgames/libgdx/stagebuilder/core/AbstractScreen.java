package net.peakgames.libgdx.stagebuilder.core;

import com.badlogic.gdx.Application.ApplicationType;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import net.peakgames.libgdx.stagebuilder.core.builder.StageBuilder;
import net.peakgames.libgdx.stagebuilder.core.util.Utils;
import net.peakgames.libgdx.stagebuilder.core.widgets.ToggleWidget;

import java.util.Map;

public abstract class AbstractScreen implements Screen {

	public static final long SCREEN_REFRESH_CHECK_PERIOD_MS = 1000;
    private static final boolean keepAspectRatio = true;
    private static final String PORTRAIT_SUFFIX = "_portrait";
    private static final String LANDSCAPE_SUFFIX = "_landscape";
    public final String TAG = getClass().getSimpleName();
    protected Graphics graphics;
    protected Stage stage;
    protected AbstractGame game;
    private AssetManager assetManager;
    private long lastScreenRefreshCheckTimestamp = System.currentTimeMillis();
    private String layoutFileChecksum;
    private boolean changesOrientation = false;
    private float fadeInDuration = Float.NEGATIVE_INFINITY;
    
    /**
     * parameters map that is used to pass configuration data for screen.
     */
    protected Map<String, String> parameters;
	private StageBuilder stageBuilder;

    public AbstractScreen(AbstractGame game) {
        this(game, false);
    }

    public AbstractScreen(AbstractGame game, boolean changesOrientation) {
        this.changesOrientation = changesOrientation;
        if (game == null) {
            return;
        }

        this.game = game;
        graphics = Gdx.graphics;

        createStage(game);

        this.assetManager = game.getAssetsInterface().getAssetMAnager();
    }

    public void enableFadeIn(float fadeInDuration) {
        this.fadeInDuration = fadeInDuration;
    }

    public void disableFadeIn() {
        fadeInDuration = Float.NEGATIVE_INFINITY;
    }

    public boolean isFadeInEnabled() {
        return fadeInDuration > 0;
    }


    private void createStage(AbstractGame game) {
        float width = game.getWidth();
        float height = game.getHeight();

        stageBuilder = new StageBuilder(game.getAssetsInterface(), game.getResolutionHelper(), game.getLocalizationService());
        stage = stageBuilder.build(getFileName(), width, height, keepAspectRatio);

        Gdx.input.setInputProcessor(this.stage);
    }

    public boolean isLandscape(){
        if(game.getWidth()>game.getHeight()){
            return true;
        } else {
            return false;
        }
    }

    private String getFileName() {
        return this.getClass().getSimpleName() + ".xml";
    }

    public abstract void unloadAssets();

    /**
     * Subclasses should override this method if they do not want to reload stage.
     * @return true if this screen should be notified be reloadStage() when screen is resized. For example when screen orientation changes.
     */
    public boolean isResizable() {
        return true;
    }

    /**
     * Stage is replaced with a new one, listeners should be updated.
     */
    public abstract void onStageReloaded();

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0f, 0f, 0f, 0f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        this.stage.act(Gdx.graphics.getDeltaTime());
        this.stage.draw();

        this.assetManager.update();
        
        refreshScreenIfNecessary();
    }

    private void refreshScreenIfNecessary() {
    	if (Gdx.app.getType() == ApplicationType.Desktop) {
    		long now = System.currentTimeMillis();
    		if (now - lastScreenRefreshCheckTimestamp > SCREEN_REFRESH_CHECK_PERIOD_MS) {
    			//check file modified date
    			String currentChecksum = calculateLayoutFileChecksum();
    			if ( ! currentChecksum.equals(this.layoutFileChecksum)) {
    				//file changed, refresh screen...
    				Gdx.app.log(TAG, "Layout file updated. Reloading stage...");
    				reloadStage();
    				this.layoutFileChecksum = currentChecksum;
    			}
    			lastScreenRefreshCheckTimestamp = now;
    		}
    	}		
	}

	@Override
    public void resize(int newWidth, int newHeight) {
        Gdx.app.log(TAG, "resize " + newWidth + " x " + newHeight);
        if (isResizable()) {
            beforeReloadStage();
            reloadStage();
            afterReloadStage();
            this.layoutFileChecksum = calculateLayoutFileChecksum();
        } else {
            Gdx.app.log(TAG, "Screen is not resizable.");
        }
    }

    /**
     * Subclasses should override this method if they want to load saved state or make changes after stage reload.
     * For example when screen orientation changes.
     */
    public void afterReloadStage() {

    }

    /**
     * Subclasses should override this method if they want to save state or make changes before stage reload.
     * For example when screen orientation changes.
     */
    public void beforeReloadStage() {

    }

    /**
     * Subclasses should override this method if they want to implement some logic before UI
     * For example preparing data for UI
     */
    public void preShow() {}

    /**
     * Subclasses should override this method if they want to implement some logic after UI
     * For example executing server responses immediately after UI created
     */
    public void postShow() {}

    @Override
    public void show() {
        preShow();
        Gdx.input.setInputProcessor(this.stage);
        Gdx.app.log(TAG, "show");
        if (isFadeInEnabled()) {
            stage.getRoot().getColor().a = 0;
            stage.addAction(Actions.fadeIn(fadeInDuration));
        }
        layoutFileChecksum = calculateLayoutFileChecksum();
        postShow();
    }

    @Override
    public void hide() {
        Gdx.input.setInputProcessor(null);
        Gdx.app.log(TAG, "hide");
    }

    @Override
    public void pause() {
        Gdx.app.log(TAG, "pause");
    }

    @Override
    public void resume() {
        Gdx.app.log(TAG, "resume");
    }

    @Override
    public void dispose() {
        Gdx.app.log(TAG, "dispose");
    }

    void setParameters(Map<String, String> parameters) {
        this.parameters = parameters;
    }

    public Image findImage(String name) {
        return (Image)findActor(name);
    }

    public Button findButton(String name) {
        return (Button)findActor(name);
    }
    
    public Label findLabel(String name) {
        return (Label)findActor(name);
    }

    public Actor findActor(String name) {
        return stage.getRoot().findActor(name);
    }

    public TextButton findTextButton(String name) {
        return (TextButton) stage.getRoot().findActor(name);
    }

    public ToggleWidget findToggleWidget(String name) {
        return (ToggleWidget) stage.getRoot().findActor(name);
    }

    public StageBuilder getStageBuilder() {
        return stageBuilder;
    }

    private String calculateLayoutFileChecksum() {
		FileHandle fileHandle = stageBuilder.getLayoutFile(getFileName());
		return Utils.calculateMD5(fileHandle.read());		
    }

    private void reloadStage() {
        Gdx.app.log(TAG, "Reloading stage.");
        createStage(game);
        onStageReloaded();
    }

    public Group getRoot() {
        return (Group) stage.getRoot().findActor(StageBuilder.ROOT_GROUP_NAME);
    }

    public boolean isChangesOrientation() {
        return changesOrientation;
    }

    public void setChangesOrientation(boolean changesOrientation) {
        this.changesOrientation = changesOrientation;
    }
}
