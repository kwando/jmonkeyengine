/*
 * Copyright (c) 2009-2012 jMonkeyEngine
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of 'jMonkeyEngine' nor the names of its contributors
 *   may be used to endorse or promote products derived from this software
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.jme3.app.state;

import com.jme3.app.Application;
import com.jme3.input.InputManager;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.post.SceneProcessor;
import com.jme3.renderer.Camera;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.Renderer;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.system.JmeSystem;
import com.jme3.texture.FrameBuffer;
import com.jme3.util.BufferUtils;
import java.io.File;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.logging.Logger;

public class ScreenshotAppState extends AbstractAppState implements ActionListener, SceneProcessor {

    private static final Logger logger = Logger.getLogger(ScreenshotAppState.class.getName());
    
    private boolean capture = false;
    private Renderer renderer;
    private RenderManager rm;
    private ByteBuffer outBuf;
    
    private int width, height;
    private NamingScheme namingScheme;
    private ScreenshotHandler handler;
    
    public static class Screenshot{
        private static int nextSequenceNumber = 1;
        private ByteBuffer buffer;
        private final int width;
        private final int height;
        private final int sequenceNumber;

        private Screenshot(ByteBuffer buffer, int width, int height) {
            this.buffer = buffer;
            this.width = width;
            this.height = height;
            sequenceNumber = nextSequenceNumber++;
        }
        
        protected ByteBuffer getBuffer(){
            return buffer;
        }
        
        public int getSequenceNumber(){
            return this.sequenceNumber;
        }
        
        public int getWidth(){
            return width;
        }
        
        public int getHeight(){
            return height;
        }
    }
    
    public interface ScreenshotHandler{
        public void screenshotCaptured(Screenshot screenshot);
    }

    private static class NamingScheme{
        private String shotName;
        private boolean numbered = true;
        private String filePath = null;
        
        /**
        * Using this constructor, the screenshot files will be written sequentially to the system
        * default storage folder.
        */
        private NamingScheme(){
        }
        
        /**
        * This constructor allows you to specify the output file path of the screenshot.
        * Include the seperator at the end of the path.
        * Use an emptry string to use the application folder. Use NULL to use the system
        * default storage folder.
        * @param filePath The screenshot file path to use. Include the seperator at the end of the path.
        */
        public NamingScheme(String filePath){
            this.filePath = filePath;
        }
        
        /**
        * This constructor allows you to specify the output file path of the screenshot.
        * Include the seperator at the end of the path.
        * Use an emptry string to use the application folder. Use NULL to use the system
        * default storage folder.
        * @param filePath The screenshot file path to use. Include the seperator at the end of the path.
        * @param shotName The name of the file to save the screeshot as.
        */
        public NamingScheme(String filePath, String shotName){
            this.filePath = filePath;
            this.shotName = shotName;
        }
        
        public File filenameForScreenshot(Screenshot screenshot) {
            File file;
            String filename;
            if (numbered) {
                filename = shotName + screenshot.getSequenceNumber();
            } else {
                filename = shotName;
            }
            if (filePath == null) {
                file = new File(JmeSystem.getStorageFolder() + File.separator + filename + ".png").getAbsoluteFile();
            } else {
                file = new File(filePath + filename + ".png").getAbsoluteFile();
            }
            return file;
        }
    }
    
    public ScreenshotAppState(NamingScheme scheme){
        super();
        this.namingScheme = scheme;
    }

    /**
     * Using this constructor, the screenshot files will be written sequentially to the system
     * default storage folder.
     */
    public ScreenshotAppState() {
        this(new NamingScheme());
    }

    /**
     * This constructor allows you to specify the output file path of the screenshot.
     * Include the seperator at the end of the path.
     * Use an emptry string to use the application folder. Use NULL to use the system
     * default storage folder.
     * @param filePath The screenshot file path to use. Include the seperator at the end of the path.
     */
    public ScreenshotAppState(String filePath) {
        this(new NamingScheme(filePath));
    }
    
    /**
     * Set the file path to store the screenshot.
     * Include the seperator at the end of the path.
     * Use an emptry string to use the application folder. Use NULL to use the system
     * default storage folder.
     * @param filePath File path to use to store the screenshot. Include the seperator at the end of the path.
     */
    public void setFilePath(String filePath) {
        namingScheme.filePath = filePath;
    }

    @Override
    public void initialize(AppStateManager stateManager, Application app) {
        if (!super.isInitialized()){
            InputManager inputManager = app.getInputManager();
            inputManager.addMapping("ScreenShot", new KeyTrigger(KeyInput.KEY_SYSRQ));
            inputManager.addListener(this, "ScreenShot");

            List<ViewPort> vps = app.getRenderManager().getPostViews();
            ViewPort last = vps.get(vps.size()-1);
            last.addProcessor(this);

            if (namingScheme.shotName == null) {
                namingScheme.shotName = app.getClass().getSimpleName();
            }
        }

        super.initialize(stateManager, app);
    }

    public void onAction(String name, boolean value, float tpf) {
        if (value){
            capture = true;
        }
    }

    public void takeScreenshot() {
        capture = true;
    }

    public void initialize(RenderManager rm, ViewPort vp) {
        renderer = rm.getRenderer();
        this.rm = rm;
        reshape(vp, vp.getCamera().getWidth(), vp.getCamera().getHeight());
    }

    @Override
    public boolean isInitialized() {
        return super.isInitialized() && renderer != null;
    }

    public void reshape(ViewPort vp, int w, int h) {
        outBuf = BufferUtils.createByteBuffer(w * h * 4);
        width = w;
        height = h;
    }

    public void preFrame(float tpf) {
    }

    public void postQueue(RenderQueue rq) {
    }

    public void postFrame(FrameBuffer out) {
        if (capture){
            capture = false;
            Screenshot screenshot = captureScreenshot(out);
            handler.screenshotCaptured(screenshot);
        }
    }

    private Screenshot captureScreenshot(FrameBuffer out) {
        Camera curCamera = rm.getCurrentCamera();
        int viewX = (int) (curCamera.getViewPortLeft() * curCamera.getWidth());
        int viewY = (int) (curCamera.getViewPortBottom() * curCamera.getHeight());
        int viewWidth = (int) ((curCamera.getViewPortRight() - curCamera.getViewPortLeft()) * curCamera.getWidth());
        int viewHeight = (int) ((curCamera.getViewPortTop() - curCamera.getViewPortBottom()) * curCamera.getHeight());
        
        renderer.setViewPort(0, 0, width, height);
        renderer.readFrameBuffer(out, outBuf);
        renderer.setViewPort(viewX, viewY, viewWidth, viewHeight);
        return new Screenshot(outBuf, width, height);
    }
}
