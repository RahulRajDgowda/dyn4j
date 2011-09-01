package org.dyn4j.sandbox.panels;

import java.awt.Window;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.DecimalFormat;
import java.text.NumberFormat;

import javax.swing.GroupLayout;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JOptionPane;

import org.dyn4j.geometry.Convex;
import org.dyn4j.geometry.Geometry;
import org.dyn4j.geometry.Polygon;
import org.dyn4j.sandbox.listeners.SelectTextFocusListener;

/**
 * Panel used to create a unit circle polygon shape.
 * @author William Bittle
 * @version 1.0.0
 * @since 1.0.0
 */
public class UnitCirclePolygonPanel extends ShapePanel implements InputPanel {
	/** the version id */
	private static final long serialVersionUID = -3651067811878657243L;

	/** The default circle radius */
	private static final double DEFAULT_RADIUS = 0.5;
	
	/** The default number of points */
	private static final int DEFAULT_COUNT = 5;
	
	/** The default circle shape */
	private static final Polygon DEFAULT_SHAPE = Geometry.createUnitCirclePolygon(DEFAULT_COUNT, DEFAULT_RADIUS);
	
	/** The circle radius */
	private double radius = DEFAULT_RADIUS;
	
	/** The point count */
	private int count = DEFAULT_COUNT;
	
	/**
	 * Default constructor.
	 */
	public UnitCirclePolygonPanel() {
		GroupLayout layout = new GroupLayout(this);
		this.setLayout(layout);
		
		JLabel lblRadius = new JLabel("Radius");
		JLabel lblCount = new JLabel("Point Count");
		
		JFormattedTextField txtRadius = new JFormattedTextField(new DecimalFormat("0.000"));
		JFormattedTextField txtCount = new JFormattedTextField(NumberFormat.getIntegerInstance());
		
		txtRadius.setValue(DEFAULT_RADIUS);
		txtCount.setValue(DEFAULT_COUNT);
		
		txtRadius.addFocusListener(new SelectTextFocusListener(txtRadius));
		txtRadius.addPropertyChangeListener("value", new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent event) {
				Number number = (Number)event.getNewValue();
				radius = number.doubleValue();
			}
		});
		
		txtCount.addFocusListener(new SelectTextFocusListener(txtCount));
		txtCount.addPropertyChangeListener("value", new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent event) {
				Number number = (Number)event.getNewValue();
				count = number.intValue();
			}
		});
		
		layout.setAutoCreateContainerGaps(true);
		layout.setAutoCreateGaps(true);
		layout.setHorizontalGroup(
				layout.createSequentialGroup()
				.addGroup(layout.createParallelGroup()
						.addComponent(lblCount)
						.addComponent(lblRadius))
				.addGroup(layout.createParallelGroup()
						.addComponent(txtCount)
						.addComponent(txtRadius)));
		layout.setVerticalGroup(
				layout.createSequentialGroup()
				.addGroup(layout.createParallelGroup()
						.addComponent(lblCount)
						.addComponent(txtCount, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
				.addGroup(layout.createParallelGroup()
						.addComponent(lblRadius)
						.addComponent(txtRadius, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)));
	}
	
	/* (non-Javadoc)
	 * @see org.dyn4j.sandbox.panels.ShapePanel#getDefaultShape()
	 */
	@Override
	public Convex getDefaultShape() {
		return DEFAULT_SHAPE;
	}
	
	/* (non-Javadoc)
	 * @see org.dyn4j.sandbox.panels.ShapePanel#getShape()
	 */
	@Override
	public Convex getShape() {
		return Geometry.createUnitCirclePolygon(this.count, this.radius);
	}
	
	/* (non-Javadoc)
	 * @see org.dyn4j.sandbox.panels.InputPanel#isValidInput()
	 */
	@Override
	public boolean isValidInput() {
		return this.radius > 0.0 && this.count >= 3;
	}
	
	/* (non-Javadoc)
	 * @see org.dyn4j.sandbox.panels.InputPanel#showInvalidInputMessage(java.awt.Window)
	 */
	@Override
	public void showInvalidInputMessage(Window owner) {
		if (!this.isValidInput()) {
			JOptionPane.showMessageDialog(owner, "A circle requires a point count greater than 2 and a radius greater than zero.", "Notice", JOptionPane.ERROR_MESSAGE);
		}
	}
}
