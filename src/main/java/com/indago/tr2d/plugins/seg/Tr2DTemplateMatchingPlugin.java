/**
 *
 */

package com.indago.tr2d.plugins.seg;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.scijava.Context;
import org.scijava.command.CommandService;
import org.scijava.log.Logger;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import com.indago.IndagoLog;
import com.indago.io.DoubleTypeImgLoader;
import com.indago.tr2d.ui.model.Tr2dModel;
import com.mycompany.imagej.TemplateMatchingPlugin;

import bdv.util.Bdv;
import bdv.util.BdvFunctions;
import bdv.util.BdvHandlePanel;
import bdv.util.BdvStackSource;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.ARGBType;
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

	private final List< Boolean > listSegmenationPerformedWithTemplateIndicator = new ArrayList< Boolean >();

	private Tr2dModel tr2dModel;

	private JTextField threshold;

	private JTextField segRad;

	private BdvHandlePanel bdv;

	private List< RandomAccessibleInterval< IntType > > segOutputs;

	private ArrayList< BdvStackSource< IntType > > overlayObjectList = null;

	private ArrayList< List< RandomAccessibleInterval< IntType > > > listofOutputsForAllTemplates =
			new ArrayList< List< RandomAccessibleInterval< IntType > > >();

	private boolean removalIndicator = false;


	@Override
	public JPanel getInteractionPanel() {
		final JPanel controls = initControlsPanel();
		bdv = initBdv( tr2dModel.getRawData() );
		return wrapToJPanel( initSplitPane( controls, bdv.getViewerPanel() ) );
	}

	private JSplitPane initSplitPane( JPanel left, JPanel right )
	{
		final JSplitPane splitPane = new JSplitPane( JSplitPane.HORIZONTAL_SPLIT, left, right );
		splitPane.setOneTouchExpandable( true );
		splitPane.setDividerLocation( 300 );
		return splitPane;
	}

	private < T > BdvHandlePanel initBdv( RandomAccessibleInterval< T > img )
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
		bStartSegmentation.addActionListener( this::onStartSegmentationButtonClicked );
		controls.add( bStartSegmentation, "growx, gapy 5 0, wrap" );
		return controls;
	}


	private JPanel initHelperPanel()
	{
		JPanel helper = new JPanel( new MigLayout() );
		helper.add( new JLabel( "Threshold:" ), "" );
		threshold = new JTextField();
		helper.add( threshold, "wrap, width 100:20" );
		helper.add( new JLabel( "Seg Radius:" ), "" );
		segRad = new JTextField();
		helper.add( segRad, "wrap, width 100:20" );
		return helper;
	}


	private JPanel initListPanel()
	{
		final JButton bAdd = initAddButton();
		final JButton bRemove = initRemoveButton();
		final JPanel list = new JPanel( new BorderLayout() );
		list.add( listTemplates, BorderLayout.CENTER );
		listTemplateSelectionListenerInit();
		list.setBorder( BorderFactory.createTitledBorder( "Templates" ) );
		JPanel helper = new JPanel( new FlowLayout( FlowLayout.RIGHT ) );

		helper.add( bAdd );
		helper.add( bRemove );
		list.add( helper, BorderLayout.SOUTH );

		JScrollPane scrollPane = new JScrollPane( listTemplates );
		list.add( scrollPane, BorderLayout.CENTER );
		return list;
	}

	private void listTemplateSelectionListenerInit() {
		listTemplates.addListSelectionListener( new ListSelectionListener() {

			@Override
			public void valueChanged( ListSelectionEvent e ) {
				if ( !e.getValueIsAdjusting() ) {
					if ( removalIndicator == false ) {
						System.out.println( removalIndicator );
						try {
							System.out.println( "I am trying dude!!!" );
							if ( listSegmenationPerformedWithTemplateIndicator.get( listTemplates.getSelectedIndex() ) ) {
								clearOverlayListAndBdvOverlay();
								showOverlay( listofOutputsForAllTemplates.get( listTemplates.getSelectedIndex() ) );
							} else {
								clearOverlayListAndBdvOverlay();
							}
						} catch ( ArrayIndexOutOfBoundsException exception ) {
							listTemplates.setSelectedIndex( 0 );
							System.out.print( "Nothing selected man!!!" );
						}

					}

				}

			}
		} );
	}

	private JButton initRemoveButton()
	{
		final JButton bRemove = new JButton( "-" );
		bRemove.addActionListener( this::onRemoveButtonClicked );
		return bRemove;
	}

	private JButton initAddButton()
	{
		final JButton bAdd = new JButton( "+" );
		bAdd.addActionListener( this::onAddButtonClicked );
		return bAdd;
	}


	public List< RandomAccessibleInterval< IntType > > fetchOutputs() {
		TemplateMatchingPlugin plugin = createTemplateMatchingPlugin();
		RandomAccessibleInterval< DoubleType > template =
				DoubleTypeImgLoader.loadTiff( new File( listTemplates.getSelectedValue() ) );
		Double matchingThreshold = Double.parseDouble( threshold.getText() );
		int segmentationRadius = Integer.parseInt( segRad.getText() );
//		List<Point> hits = plugin.calculatePoints( tr2dModel.getRawData(), template, segmentationRadius, matchingThreshold );
//		return plugin.createSegmentation( hits, tr2dModel.getRawData(), segmentationRadius );
		segOutputs = plugin.calculate( tr2dModel.getRawData(), template, segmentationRadius, matchingThreshold );
		listSegmenationPerformedWithTemplateIndicator.set( listTemplates.getSelectedIndex(), true );
		return segOutputs;

	}

	@Override
	public List< RandomAccessibleInterval< IntType > > getOutputs() {
		return segOutputs;

	}

	private TemplateMatchingPlugin createTemplateMatchingPlugin() {
		TemplateMatchingPlugin plugin = new TemplateMatchingPlugin();
		context.inject( plugin );
		return plugin;
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

	public void onAddButtonClicked( ActionEvent e ) {
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
				listSegmenationPerformedWithTemplateIndicator.add( false );
				System.out.println( model.getSize() );
			}
		}
	}

	public void onRemoveButtonClicked( ActionEvent e ) {
		int idx = listTemplates.getSelectedIndex();
		removalIndicator = true;
		model.remove( idx );
		try {
			listSegmenationPerformedWithTemplateIndicator.remove( idx );
			listofOutputsForAllTemplates.remove( idx );
		} catch ( IndexOutOfBoundsException exception ) {
			System.out.println( "Hey! This was not even initialized!!!" );
		}

		removalIndicator = false;
	}


	public void onStartSegmentationButtonClicked( ActionEvent e ) {
		clearOverlayListAndBdvOverlay();
		List< RandomAccessibleInterval< IntType > > outputs = fetchOutputs();
		showOverlay( outputs );
		listofOutputsForAllTemplates.add( outputs );

	}

	private void showOverlay( List< RandomAccessibleInterval< IntType > > outputs ) {
		int overlayBucketSize = outputs.size();
		overlayObjectList = new ArrayList< BdvStackSource< IntType > >( overlayBucketSize );
		List< Integer > color = new ArrayList< Integer >();
		color.add( 0xFF00FF00 );
		color.add( 0xFFFF0000 );
		color.add( 0xFFFFFF00 );
		color.add( 0xFFFF00FF );
		color.add( 0x0000FF );
		int count = -1;

		for ( RandomAccessibleInterval< IntType > output : outputs ) {
			BdvStackSource< IntType > entry = BdvFunctions.show( output, "Overlays", Bdv.options().addTo( bdv ) );
			overlayObjectList.add( entry );
			count += 1;
			entry.setColor( new ARGBType( color.get( count ) ) );
			entry.setDisplayRange( 0, 0 );
		}
	}


	private void clearOverlayListAndBdvOverlay() {

		if ( overlayObjectList != null ) {
			for ( BdvStackSource< IntType > overlayObject : overlayObjectList ) {
				overlayObject.removeFromBdv();
			}
			overlayObjectList.clear();
		}

	}


}
