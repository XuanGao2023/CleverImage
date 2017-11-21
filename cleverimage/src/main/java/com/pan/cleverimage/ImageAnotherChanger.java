package com.pan.cleverimage;

/**
 * Created by pan on 21/11/2017.
 */

public class ImageAnotherChanger extends ImageGetter {

    public static ImageGetter init() {
        return init(new ImageAnotherChanger());
    }

    protected ImageAnotherChanger() {
        super();
    }
}
