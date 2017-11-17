package com.popland.pop.sum;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

/**
 * Created by hai on 16/11/2017.
 */

public class FocusBox extends View {
    Paint paint;
    int MIN_FOCUSBOX_WIDTH = 100;
    int MIN_FOCUSBOX_HEIGHT = 100;
    int L,T,R,B;
    int layoutW =0, layoutH=0;
    public FocusBox(Context context, AttributeSet attrs) {
        super(context,attrs);
        paint = new Paint();
    }


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        //draw default frame
        if(layoutW==0){
            layoutW = canvas.getWidth();//get FrameLayout's size
            layoutH = canvas.getHeight();
            //phone screen sizes range from 240 X 320 to 1440 X 2560
            //put box in center
            int w = layoutW/10;
            int h = layoutH/10;
            w = (w>=0 && w<MIN_FOCUSBOX_WIDTH) ? MIN_FOCUSBOX_WIDTH : w;
            h = (h>=0 && h<MIN_FOCUSBOX_HEIGHT) ? MIN_FOCUSBOX_HEIGHT : h;
            L = layoutW/2 - w/2;
            T = layoutH/2 - h/2;
            R = L + w;
            B = T + h;
        }
        //draw darked exterior rects
        paint.setColor(Color.parseColor("#60000000"));
        canvas.drawRect(0, 0, layoutW, T, paint);
        canvas.drawRect(0, T, L, B, paint);
        canvas.drawRect(R, T, layoutW, B, paint);
        canvas.drawRect(0, B, layoutW, layoutH, paint);

        //draw focus box
        paint.setColor(Color.parseColor("#fcfdfd"));
        paint.setStyle(Paint.Style.FILL);
        canvas.drawRect(L, T, R, T + 1, paint);
        canvas.drawRect(L, T + 1, L + 1, B - 1, paint);
        canvas.drawRect(R - 1, T + 1, R, B - 1, paint);
        canvas.drawRect(L, B - 1, R, B, paint);

        canvas.drawCircle(L, T, 20, paint);
        canvas.drawCircle(R, T, 20, paint);
        canvas.drawCircle(R, B, 20, paint);
        canvas.drawCircle(L, B, 20, paint);
    }

    public Rect getBox(){
        Rect rect = new Rect(L,T,R,B);
        return rect;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if(event.getAction()==MotionEvent.ACTION_MOVE){
            int X = (int) event.getX();
            int Y = (int) event.getY();
            if(X>=100 && X<=layoutW-100 && Y>=100 && Y<=layoutH-100) {
                if (X <= L + 50 && X >= L - 50 && Y <= T + 50 && Y >= T - 50) {
                    if (X <= R - MIN_FOCUSBOX_WIDTH && Y <= B - MIN_FOCUSBOX_HEIGHT) {
                        L = X;
                        T = Y;
                        invalidate();
                    }
                } else if (X <= R + 50 && X >= R - 50 && Y <= T + 50 && Y >= T - 50) {
                    if (X >= L + MIN_FOCUSBOX_WIDTH && Y <= B - MIN_FOCUSBOX_HEIGHT) {
                        R = X;
                        T = Y;
                        invalidate();
                    }
                } else if (X <= R + 50 && X >= R - 50 && Y <= B + 50 && Y >= B - 50) {
                    if (X >= L + MIN_FOCUSBOX_WIDTH && Y >= T + MIN_FOCUSBOX_HEIGHT) {
                        R = X;
                        B = Y;
                        invalidate();
                    }
                } else if (X <= L + 50 && X >= L - 50 && Y <= B + 50 && Y >= B - 50) {
                    if (X <= R - MIN_FOCUSBOX_WIDTH && Y >= T + MIN_FOCUSBOX_HEIGHT) {
                        L = X;
                        B = Y;
                        invalidate();
                    }
                }
            }
        }
        return true;
    }

}

