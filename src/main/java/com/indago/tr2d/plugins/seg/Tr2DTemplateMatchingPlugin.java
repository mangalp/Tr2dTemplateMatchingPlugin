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
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import org.scijava.command.CommandService;
import org.scijava.log.Logger;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import com.indago.IndagoLog;
import com.indago.tr2d.ui.model.Tr2dModel;
import com.indago.tr2d.ui.util.JDoubleListTextPane;
import com.mycompany.imagej.TemplateMatchingPlugin;

import bdv.util.Bdv;
import bdv.util.BdvFunctions;
import bdv.util.BdvHandlePanel;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.integer.IntType;
import net.imglib2.type.numeric.real.DoubleType;
import net.miginfocom.swing.MigLayout;


/**
 * @author Mangal Prakash
 */
@Plugin( type = Tr2dSegmentationPlugin.class, name = "Tr2d Template Matching Segmentation" )
public class Tr2DTemplateMatchingPlugin implements Tr2dSegmentationPlugin, AutoCloseable {

	@Parameter
	private Context context;

	@Parameter
	private CommandService commandService;

	public Logger log = IndagoLog.stdLogger().subLogger( "Tr2dTemplateMatchingPlugin" );

	DefaultListModel< String > model = new DefaultListModel<>();
	private final JList< String > listTemplates = new JList<>( model );

	private final JDoubleListTextPane txtThreshold = new JDoubleListTextPane();
	private Tr2dModel tr2dModel;

	@Override
	public JPanel getInteractionPanel() {
		final JPanel controls = initControlsPanel();
		final BdvHandlePanel bdv = initBdv( tr2dModel.getRawData() );
		return wrapToJPanel( initSplitPane( controls, bdv ) );
	}

	private JSplitPane initSplitPane( JPanel controls, BdvHandlePanel bdv )
	{
		final JSplitPane splitPane = new JSplitPane( JSplitPane.HORIZONTAL_SPLIT, controls, bdv.getBdvHandle().getViewerPanel() );
		splitPane.setOneTouchExpandable( true );
		splitPane.setDividerLocation( 300 );
		return splitPane;
	}

	private BdvHandlePanel initBdv( RandomAccessibleInterval< DoubleType > img )
	{
		final BdvHandlePanel bdv = new BdvHandlePanel( null, Bdv.options().is2D() );
		BdvFunctions.show( img, "img", Bdv.options().addTo( bdv ) );
		return bdv;
	}

	private JPanel wrapToJPanel( JSplitPane splitPane )
	{
		final JPanel splittedPanel = new JPanel();
		splittedPanel.setLayout( new BorderLayout() );
		splittedPanel.add( splitPane, BorderLayout.CENTER );
		return splittedPanel;
	}

	private JPanel initControlsPanel()
	{
		final MigLayout layout = new MigLayout( "fill", "[grow]", "" );
		final JPanel controls = new JPanel( layout );

		final JPanel list = initListPanel();
		controls.add( list, "h 100%, grow, wrap" );

		JPanel helper = initHelperPanel();
		controls.add( helper, "growx, wrap" );

		JButton bStartSegmentation = new JButton( "start matching with selected template" );
		controls.add( bStartSegmentation, "growx, gapy 5 0, wrap" );
		return controls;
	}

	private JPanel initHelperPanel()
	{
		JPanel helper = new JPanel( new BorderLayout() );
		helper.add( new JLabel( "Thresholds:" ), BorderLayout.WEST );
		JDoubleListTextPane txtThresholds = new JDoubleListTextPane();
		txtThresholds.setEnabled( false );
		helper.add( txtThresholds, BorderLayout.CENTER );
		return helper;
	}

	private JPanel initListPanel()
	{
		final JButton bAdd = initAddButton();
		final JButton bRemove = initRemoveButton();
		final JPanel list = new JPanel( new BorderLayout() );
		list.add( listTemplates, BorderLayout.CENTER );
		list.setBorder( BorderFactory.createTitledBorder( "Templates" ) );
		JPanel helper = new JPanel( new FlowLayout( FlowLayout.RIGHT ) );

		helper.add( bAdd );
		helper.add( bRemove );
		list.add( helper, BorderLayout.SOUTH );

		JScrollPane scrollPane = new JScrollPane( listTemplates );
		list.add( scrollPane, BorderLayout.CENTER );
		return list;
	}

	private JButton initRemoveButton()
	{
		final JButton bRemove = new JButton( "-" );
		bRemove.addActionListener( new ButtonListener() );
		return bRemove;
	}

	private JButton initAddButton()
	{
		final JButton bAdd = new JButton( "+" );
		bAdd.addActionListener( new ButtonListener() );
		return bAdd;
	}

	@Override
	public List< RandomAccessibleInterval< IntType > > getOutputs() {

		//Return type of Fiji Plugin TemplateMatching method needs to change to List of RAIs//
		//Or can any other method of Fiji Plugin be invoked bypassing run???
		Map< String, Object > inputMap = new HashMap();
		inputMap.put( "inputImage", tr2dModel.getRawData() );
		inputMap.put( "inputTemplate", new File( model.get( 0 ) ) );
		try {
			inputMap.put( "saveResultsDir", tr2dModel.getProjectFolder().addFolder( "Template Matching Segmentations" ).getFolder() );
		} catch ( IOException e ) {
			e.printStackTrace();
		}
		inputMap.put( "segCircleRad", 6 );
		commandService.run( TemplateMatchingPlugin.class, false, inputMap );
		return null; //panel.getOutputs();
	}

	@Override
	public void setTr2dModel(final Tr2dModel model) {
		this.tr2dModel = model;
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
						System.out.println( model.getSize() );
					}
				}

			}
			if(e.getActionCommand().equals( "-" )) {

				int[] idxs = listTemplates.getSelectedIndices();
				for ( int index = 0; index < idxs.length; index++ ) {
					if ( index >= 0 ) {
						model.remove( index );
					}

				}
				

			}
			if ( e.getActionCommand().equals( "start matching with selected template" ) ) {
				// Do template Matching with the given threshold 
				
			}
		}
	}
}
