package net.nightweb;

public class LinearLayout extends android.widget.LinearLayout {
	public LinearLayout (android.content.Context context) {
		super(context);
	}

	public LinearLayout (android.content.Context context, android.util.AttributeSet attrs) {
		super(context, attrs);
	}

	public LinearLayout (android.content.Context context, android.util.AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	public final void superSetMeasuredDimension(int measuredWidth, int measuredHeight) {
		super.setMeasuredDimension(measuredWidth, measuredHeight);
	}

	public void superSetChildrenDrawingOrderEnabled(boolean enabled) {
		super.setChildrenDrawingOrderEnabled(enabled);
	}
}
