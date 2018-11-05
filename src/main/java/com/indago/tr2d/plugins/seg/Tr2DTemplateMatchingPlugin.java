package com.indago.tr2d.plugins.seg;

import com.indago.IndagoLog;
import com.indago.tr2d.ui.model.Tr2dModel;
import com.indago.template_matching.TemplateMatchingPanel;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.integer.IntType;
import org.scijava.Context;
import org.scijava.log.Logger;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import javax.swing.*;
import java.util.List;

@Plugin( type = Tr2dSegmentationPlugin.class, name = "Tr2d Template Matching Segmentation" )
public class Tr2DTemplateMatchingPlugin implements Tr2dSegmentationPlugin
{
	@Parameter
	Context context;

	TemplateMatchingPanel panel;

	@Override
	public JPanel getInteractionPanel()
	{
		return panel.getPanel();
	}

	@Override
	public List< RandomAccessibleInterval< IntType > > getOutputs()
	{
		return panel.getOutputs();
	}

	@Override
	public void setTr2dModel( Tr2dModel tr2dModel )
	{
		Logger log = IndagoLog.stdLogger().subLogger( "Tr2dTemplateMatchingPlugin" );
		panel = new TemplateMatchingPanel( tr2dModel.getRawData(), context, log );
	}

	@Override
	public String getUiName()
	{
		return "template Matching segmentation";
	}

	@Override
	public void setLogger( Logger logger )
	{
		panel.setLogger( logger );
	}

	@Override
	public boolean isUsable()
	{
		return true;
	}

	@Override
	public void close()
	{
		panel.close();
	}
}
