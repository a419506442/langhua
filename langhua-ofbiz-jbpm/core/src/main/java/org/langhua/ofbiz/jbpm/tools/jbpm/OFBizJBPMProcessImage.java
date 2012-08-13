/*
 * This library is part of OFBiz-jBPM Component of Langhua
 *
 * Copyright (C) 2010  Langhua Opensource Foundation (http://langhua.org)
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3.0 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * For the latest version about this component, please see the
 * project website: http://langhua.org/opensource/ofbiz/ofbiz-jbpm/
 * 
 * For more information on Apache OFBiz, please see the
 * project website: http://ofbiz.apache.org/
 * 
 * For more information on JBPM, please see the
 * project website: http://www.jboss.org/jbpm/
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.langhua.ofbiz.jbpm.tools.jbpm;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.TagSupport;

import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.XPath;
import org.dom4j.xpath.DefaultXPath;
import org.jbpm.JbpmConfiguration;
import org.jbpm.JbpmContext;
import org.jbpm.file.def.FileDefinition;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.Token;
import org.jbpm.taskmgmt.exe.TaskInstance;

public class OFBizJBPMProcessImage extends TagSupport {

	private static final long serialVersionUID = 1L;
	private long taskInstanceId = -1;
	private long tokenInstanceId = -1;

	private byte[] gpdBytes = null;
	private byte[] imageBytes = null;
	private Token currentToken = null;
	private ProcessDefinition processDefinition = null;

	static String currentTokenColor = "red";
	static String childTokenColor = "blue";
	static String tokenNameColor = "blue";

	public void release() {
		taskInstanceId = -1;
		gpdBytes = null;
		imageBytes = null;
		currentToken = null;
	}

	public int doEndTag() throws JspException {
		try {
			initialize();
			retrieveByteArrays();
			if (gpdBytes != null && imageBytes != null) {
				writeTable();
			}
		} catch (IOException e) {
			e.printStackTrace();
			throw new JspException("table couldn't be displayed", e);
		} catch (DocumentException e) {
			e.printStackTrace();
			throw new JspException("table couldn't be displayed", e);
		}
		release();
		return EVAL_PAGE;
	}

	private void retrieveByteArrays() {
		try {
			FileDefinition fileDefinition = processDefinition
					.getFileDefinition();
			gpdBytes = fileDefinition.getBytes("gpd.xml");
			imageBytes = fileDefinition.getBytes("processimage.jpg");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void writeTable() throws IOException, DocumentException {

		int borderWidth = 4;
		Element rootDiagramElement = DocumentHelper.parseText(
				new String(gpdBytes)).getRootElement();
		int[] boxConstraint;
		int[] imageDimension = extractImageDimension(rootDiagramElement);
		String imageLink = ((HttpServletRequest) pageContext.getRequest())
				.getContextPath()
				+ "/processimage?definitionId=" + processDefinition.getId();
		JspWriter jspOut = pageContext.getOut();

		if (tokenInstanceId > 0) {

			List allTokens = new ArrayList();
			walkTokens(currentToken, allTokens);

			jspOut
					.println("<div style='position:relative; background-image:url("
							+ imageLink
							+ "); width: "
							+ imageDimension[0]
							+ "px; height: " + imageDimension[1] + "px;'>");

			for (int i = 0; i < allTokens.size(); i++) {
				Token token = (Token) allTokens.get(i);

				// check how many tokens are on teh same level (= having the
				// same parent)
				int offset = i;
				if (i > 0) {
					while (offset > 0
							&& ((Token) allTokens.get(offset - 1)).getParent()
									.equals(token.getParent())) {
						offset--;
					}
				}
				boxConstraint = extractBoxConstraint(rootDiagramElement, token);

				// Adjust for borders
				// boxConstraint[2]-=borderWidth*2;
				// boxConstraint[3]-=borderWidth*2;

				jspOut.println("<div style='position:absolute; left: "
						+ boxConstraint[0] + "px; top: " + boxConstraint[1]
						+ "px; ");

				if (i == (allTokens.size() - 1)) {
					jspOut.println("border: " + currentTokenColor);
				} else {
					jspOut.println("border: " + childTokenColor);
				}

				jspOut.println(" " + borderWidth + "px groove; " + "width: "
						+ boxConstraint[2] + "px; height: " + boxConstraint[3]
						+ "px;'>");

				if (token.getName() != null) {
					jspOut.println("<span style='color:" + tokenNameColor
							+ ";font-style:italic;position:absolute;left:"
							+ (boxConstraint[2] + 10) + "px;top:"
							+ ((i - offset) * 20) + ";'>&nbsp;"
							+ token.getName() + "</span>");
				}

				jspOut.println("</div>");
			}
			jspOut.println("</div>");
		} else {
			boxConstraint = extractBoxConstraint(rootDiagramElement);

			jspOut.println("<table border=0 cellspacing=0 cellpadding=0 width="
					+ imageDimension[0] + " height=" + imageDimension[1] + ">");
			jspOut.println("  <tr>");
			jspOut.println("    <td width=" + imageDimension[0] + " height="
					+ imageDimension[1] + " style=\"background-image:url("
					+ imageLink + ")\" valign=top>");
			jspOut
					.println("      <table border=0 cellspacing=0 cellpadding=0>");
			jspOut.println("        <tr>");
			jspOut.println("          <td width="
					+ (boxConstraint[0] - borderWidth) + " height="
					+ (boxConstraint[1] - borderWidth)
					+ " style=\"background-color:transparent;\"></td>");
			jspOut.println("        </tr>");
			jspOut.println("        <tr>");
			jspOut
					.println("          <td style=\"background-color:transparent;\"></td>");
			jspOut
					.println("          <td style=\"border-color:"
							+ currentTokenColor
							+ "; border-width:"
							+ borderWidth
							+ "px; border-style:groove; background-color:transparent;\" width="
							+ boxConstraint[2] + " height="
							+ (boxConstraint[3] + (2 * borderWidth))
							+ ">&nbsp;</td>");
			jspOut.println("        </tr>");
			jspOut.println("      </table>");
			jspOut.println("    </td>");
			jspOut.println("  </tr>");
			jspOut.println("</table>");
		}
	}

	private int[] extractBoxConstraint(Element root) {
		return extractBoxConstraint(root, currentToken);
	}

	private int[] extractBoxConstraint(Element root, Token token) {
		int[] result = new int[4];
		String nodeName = token.getNode().getName();
		XPath xPath = new DefaultXPath("//node[@name='" + nodeName + "']");
		Element node = (Element) xPath.selectSingleNode(root);
		result[0] = Integer.valueOf(node.attribute("x").getValue()).intValue();
		result[1] = Integer.valueOf(node.attribute("y").getValue()).intValue();
		result[2] = Integer.valueOf(node.attribute("width").getValue())
				.intValue();
		result[3] = Integer.valueOf(node.attribute("height").getValue())
				.intValue();
		if (result[2] < 0)
			result[2] = -1 * result[2];
		if (result[3] < 0)
			result[3] = -1 * result[3];
		return result;
	}

	private int[] extractImageDimension(Element root) {
		int[] result = new int[2];
		result[0] = Integer.valueOf(root.attribute("width").getValue())
				.intValue();
		result[1] = Integer.valueOf(root.attribute("height").getValue())
				.intValue();
		return result;
	}

	private void initialize() {
		JbpmContext context = JbpmConfiguration.getInstance()
				.getCurrentJbpmContext();
		if (context == null) {
			context = JbpmConfiguration.getInstance().createJbpmContext();
		}
		if (this.taskInstanceId > 0) {
			TaskInstance taskInstance = context.getTaskMgmtSession()
					.loadTaskInstance(taskInstanceId);
			currentToken = taskInstance.getToken();
		} else {
			if (this.tokenInstanceId > 0)
				currentToken = context.getGraphSession().loadToken(
						this.tokenInstanceId);
		}
		processDefinition = currentToken.getProcessInstance()
				.getProcessDefinition();
	}

	private void walkTokens(Token parent, List allTokens) {
		Map children = parent.getChildren();
		if (children != null && children.size() > 0) {
			Collection childTokens = children.values();
			for (Iterator iterator = childTokens.iterator(); iterator.hasNext();) {
				Token child = (Token) iterator.next();
				walkTokens(child, allTokens);
			}
		}

		allTokens.add(parent);
	}

	public void setTask(long id) {
		this.taskInstanceId = id;
	}

	public void setToken(long id) {
		this.tokenInstanceId = id;
	}

}
