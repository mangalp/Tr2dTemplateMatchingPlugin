/**
 *
 */

package com.indago.tr2d.plugins.seg;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.io.File;
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

	private Tr2dModel tr2dModel;

	private JTextField threshold;

	private JTextField segRad;

	private BdvHandlePanel bdv;

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
		BdvFunctions.show( img, "img", Bdv.options().addTo( bdv ) ).setColor( new ARGBType( 0xFFFFFF00 ) );;
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
		bRemove.addActionListener( this::onRemoveButtonClicked );
		return bRemove;
	}

	private JButton initAddButton()
	{
		final JButton bAdd = new JButton( "+" );
		bAdd.addActionListener( this::onAddButtonClicked );
		return bAdd;
	}

	@Override
	public List< RandomAccessibleInterval< IntType > > getOutputs() {
		TemplateMatchingPlugin plugin = createTemplateMatchingPlugin();
		RandomAccessibleInterval< DoubleType > template =
				DoubleTypeImgLoader.loadTiff( new File( listTemplates.getSelectedValue() ) );
		Double matchingThreshold = Double.parseDouble( threshold.getText() );
		int segmentationRadius = Integer.parseInt( segRad.getText() );
//		List<Point> hits = plugin.calculatePoints( tr2dModel.getRawData(), template, segmentationRadius, matchingThreshold );
//		return plugin.createSegmentation( hits, tr2dModel.getRawData(), segmentationRadius );
		return plugin.calculate( tr2dModel.getRawData(), template, segmentationRadius, matchingThreshold );

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
				System.out.println( model.getSize() );
			}
		}
	}

	public void onRemoveButtonClicked( ActionEvent e ) {
		int[] idxs = listTemplates.getSelectedIndices();
		for ( int index = 0; index < idxs.length; index++ ) {
			if ( index >= 0 ) {
				model.remove( index );
			}
		}
	}

	public void onStartSegmentationButtonClicked( ActionEvent e ) {
		List< RandomAccessibleInterval< IntType > > a = getOutputs();
		int size = a.size();
		BdvFunctions.show( a.get( size - 1 ), "reds", Bdv.options().addTo( bdv ) ).setColor( new ARGBType( 0xFF00FF00 ) );;

	}




}
