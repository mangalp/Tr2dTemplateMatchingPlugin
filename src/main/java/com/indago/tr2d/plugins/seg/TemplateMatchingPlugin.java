/**
 *
 */

package com.indago.tr2d.plugins.seg;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;

import org.scijava.Context;
import org.scijava.log.Logger;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import com.indago.IndagoLog;
import com.indago.tr2d.ui.model.Tr2dModel;
import com.indago.tr2d.ui.util.JDoubleListTextPane;

import bdv.util.Bdv;
import bdv.util.BdvFunctions;
import bdv.util.BdvHandlePanel;
import ij.ImagePlus;
import indago.ui.progress.DialogProgress;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.numeric.integer.IntType;
import net.miginfocom.swing.MigLayout;


/**
 * @author Mangal Prakash
 */
@Plugin( type = Tr2dSegmentationPlugin.class, name = "Tr2d Template Matching Segmentation" )
public class TemplateMatchingPlugin implements Tr2dSegmentationPlugin, AutoCloseable {

	@Parameter
	private Context context;
	public Logger log = IndagoLog.stdLogger().subLogger( "Tr2dTemplateMatchingPlugin" );

	DefaultListModel< String > model = new DefaultListModel<>();
	private final JList< String > listTemplates = new JList<>( model );
//	private final JList< String > listTemplates = new JList<>();

	@Override
	public JPanel getInteractionPanel() {
//		final JList< String > listTemplates = new JList<>();
		final JButton bAdd = new JButton( "+" );
		final JButton bRemove = new JButton( "-" );

		JLabel lblThresholds;
		JDoubleListTextPane txtThresholds;

		JButton bStartSegmentation;

		DialogProgress trackingProgressDialog = null;

		final MigLayout layout = new MigLayout( "fill", "[grow]", "" );
		final JPanel controls = new JPanel( layout );

		final JPanel list = new JPanel( new BorderLayout() );
//		listTemplates.addListSelectionListener( new ListSelectionListener() {
//
//			@Override
//			public void valueChanged( ListSelectionEvent e ) {
//				if ( e.getValueIsAdjusting() )
//					return;
//
//			}
//
//		} );
		list.add( listTemplates, BorderLayout.CENTER );
		list.setBorder( BorderFactory.createTitledBorder( "Templates" ) );
		JPanel helper = new JPanel( new FlowLayout( FlowLayout.RIGHT ) );

		helper.add( bAdd );
		helper.add( bRemove );
		list.add( helper, BorderLayout.SOUTH );

		JScrollPane scrollPane = new JScrollPane( listTemplates );
		list.add( scrollPane, BorderLayout.CENTER );
		controls.add( list, "h 100%, grow, wrap" );

		helper = new JPanel( new BorderLayout() );
		lblThresholds = new JLabel( "Thresholds:" );
		txtThresholds = new JDoubleListTextPane();
		txtThresholds.setEnabled( false );
		helper.add( lblThresholds, BorderLayout.WEST );
		helper.add( txtThresholds, BorderLayout.CENTER );
		controls.add( helper, "growx, wrap" );

		bStartSegmentation = new JButton( "start matching with selected template" );
		controls.add( bStartSegmentation, "growx, gapy 5 0, wrap" );

		bAdd.addActionListener( new ButtonListener() );

		final JFrame frame = new JFrame();
		String path = "/Users/prakash/Desktop/LabkitTestFolder";
		ImagePlus imagePlus = new ImagePlus( path + "/raw.tif" );
		Img img = ImageJFunctions.wrapReal( imagePlus );
		final BdvHandlePanel bdv = new BdvHandlePanel( frame, Bdv.options().is2D() );
		BdvFunctions.show( img, "img", Bdv.options().addTo( bdv ) );
		final JSplitPane splitPane = new JSplitPane( JSplitPane.HORIZONTAL_SPLIT, controls, bdv.getBdvHandle().getViewerPanel() );
		splitPane.setOneTouchExpandable( true );
		splitPane.setDividerLocation( 300 );

		final JPanel splittedPanel = new JPanel();
		splittedPanel.add( splitPane, BorderLayout.CENTER );
		splittedPanel.setPreferredSize( new Dimension( 500, 500 ) );

		return splittedPanel;
//		return controls;
	}

	@Override
	public List<RandomAccessibleInterval<IntType>> getOutputs() {
		return null; //panel.getOutputs();
	}

	@Override
	public void setTr2dModel(final Tr2dModel model) {
	}

	@Override
	public String getUiName() {
		return "template Matching segmentation";
	}

	@Override
	public void setLogger(Logger logger) {
		log = logger;
	}

	@Override
	public boolean isUsable() {
		return true; //panel.isUsable();
	}

	@Override
	public void close() {
		// panel.close();
	}

	class ButtonListener implements ActionListener {

		public ButtonListener() {
			// TODO Auto-generated constructor stub
		}

		@Override
		public void actionPerformed( ActionEvent e ) {
			if ( e.getActionCommand().equals( "+" ) ) {
				JFileChooser fileChooser = new JFileChooser();
				int returnName = fileChooser.showOpenDialog( null );
				String path;

				if ( returnName == JFileChooser.APPROVE_OPTION ) {
					File f = fileChooser.getSelectedFile();
					if ( f != null ) { // Make sure the user didn't choose a directory.

						path = f.getAbsolutePath();//get the absolute path to selected file
						//below line to test the file chooser
						System.out.println( path );
						model.addElement( path );
//						listTemplates.setSelectedIndex( listTemplates.getModel().getSize() - 1 );
					}
				}

			}
			if(e.getActionCommand().equals( "-" )) {
				int [] idxs = listTemplates.getSelectedIndices();
				for ( int index = 0; index < idxs.length; index++ ) {
					if ( index >= 0 ) {
						model.remove( index );
					}

				}
				

			}
		}
	}
}
