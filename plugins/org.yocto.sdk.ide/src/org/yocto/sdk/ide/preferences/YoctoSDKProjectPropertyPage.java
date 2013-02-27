/*******************************************************************************
 * Copyright (c) 2012 BMW Car IT GmbH.
 * Copyright (c) 2010 Intel.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * BMW Car IT GmbH - initial implementation
 * Intel - initial API implementation (copied from YoctoSDKPreferencePage)
 *******************************************************************************/
package org.yocto.sdk.ide.preferences;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbenchPropertyPage;
import org.eclipse.ui.dialogs.PropertyPage;
import org.yocto.sdk.ide.YoctoGeneralException;
import org.yocto.sdk.ide.YoctoProfileElement;
import org.yocto.sdk.ide.YoctoProfileSetting;
import org.yocto.sdk.ide.YoctoProjectSpecificSetting;
import org.yocto.sdk.ide.YoctoSDKChecker.SDKCheckRequestFrom;
import org.yocto.sdk.ide.YoctoSDKPlugin;
import org.yocto.sdk.ide.YoctoSDKUtils;
import org.yocto.sdk.ide.YoctoUIElement;
import org.yocto.sdk.ide.YoctoUISetting;

public class YoctoSDKProjectPropertyPage extends PropertyPage implements
		IWorkbenchPropertyPage {

	private YoctoProfileSetting yoctoProfileSetting;
	private YoctoProjectSpecificSetting yoctoProjectSpecificSetting;
	private YoctoUISetting yoctoUISetting;
	private IProject project = null;

	@Override
	protected Control createContents(Composite parent) {
		IProject project = getProject();

		YoctoProfileElement globalProfileElement= YoctoSDKUtils.getProfilesFromDefaultStore();
		YoctoProfileElement profileElement = YoctoSDKUtils.getProfilesFromProjectPreferences(project);

		String selectedProfile = profileElement.getSelectedProfile();
		if (!globalProfileElement.contains(selectedProfile)) {
			selectedProfile = globalProfileElement.getSelectedProfile();
		}

		yoctoProfileSetting = new YoctoProfileSetting(
				new YoctoProfileElement(globalProfileElement.getProfilesAsString(), selectedProfile), this, false);
		boolean useProjectSpecificSetting = YoctoSDKUtils.getUseProjectSpecificOptionFromProjectPreferences(project);

		if (useProjectSpecificSetting) {
			yoctoUISetting = new YoctoUISetting(YoctoSDKUtils.getElemFromProjectPreferences(project));
		} else {
			yoctoUISetting = new YoctoUISetting(YoctoSDKUtils.getElemFromStore(YoctoSDKPlugin.getProfilePreferenceStore(selectedProfile)));
		}

		yoctoProjectSpecificSetting = new YoctoProjectSpecificSetting(yoctoProfileSetting, yoctoUISetting, this);

		initializeDialogUnits(parent);
		final Composite result = new Composite(parent, SWT.NONE);

		yoctoProfileSetting.createComposite(result);
		yoctoProjectSpecificSetting.createComposite(result);

		try {
			yoctoUISetting.createComposite(result);

			if (useProjectSpecificSetting) {
				yoctoProfileSetting.setUIFormEnabledState(false);
				yoctoProjectSpecificSetting.setUseProjectSpecificSettings(true);
				yoctoUISetting.setUIFormEnabledState(true);
				yoctoUISetting.validateInput(SDKCheckRequestFrom.Preferences, false);
			} else {
				yoctoProfileSetting.setUIFormEnabledState(true);
				yoctoProjectSpecificSetting.setUseProjectSpecificSettings(false);
				yoctoUISetting.setUIFormEnabledState(false);
			}

			Dialog.applyDialogFont(result);
			return result;
		} catch (YoctoGeneralException e) {
			System.out.println("Have you ever set Yocto Project Reference before?");
			System.out.println(e.getMessage());
			return result;
		}
	}

	private IProject getProject() {
		if (project != null) {
			return project;
		}

		IAdaptable adaptable = getElement();
		if (adaptable == null) {
			throw new IllegalStateException("Project can only be retrieved after properties page has been set up.");
		}

		project = (IProject) adaptable.getAdapter(IProject.class);
		return project;
	}

	/*
	 * @see PreferencePage#performDefaults()
	 */
	@Override
	protected void performDefaults() {
		YoctoUIElement defaultElement = YoctoSDKUtils.getDefaultElemFromDefaultStore();
		yoctoUISetting.setCurrentInput(defaultElement);
		yoctoProjectSpecificSetting.setUseProjectSpecificSettings(true);
		super.performDefaults();
	}

	/*
	 * @see IPreferencePage#performOk()
	 */
	@Override
	public boolean performOk() {
		try {
			IProject project = getProject();

			if (yoctoProjectSpecificSetting.isUsingProjectSpecificSettings()) {
				yoctoUISetting.validateInput(SDKCheckRequestFrom.Preferences, true);

				YoctoSDKUtils.saveUseProjectSpecificOptionToProjectPreferences(project, true);
				YoctoSDKUtils.saveProfilesToProjectPreferences(yoctoProfileSetting.getCurrentInput(), project);
				YoctoSDKUtils.saveElemToProjectPreferences(yoctoUISetting.getCurrentInput(), project);
			} else {
				YoctoSDKUtils.saveUseProjectSpecificOptionToProjectPreferences(project, false);
				YoctoSDKUtils.saveProfilesToProjectPreferences(yoctoProfileSetting.getCurrentInput(), project);
			}

			YoctoSDKUtils.saveElemToProjectEnv(yoctoUISetting.getCurrentInput(), getProject());

			return super.performOk();
		} catch (YoctoGeneralException e) {
			// TODO Auto-generated catch block
			System.out.println(e.getMessage());
			return false;
		}
	}

	public void switchProfile(String selectedProfile)
	{
		YoctoUIElement profileElement = YoctoSDKUtils.getElemFromStore(YoctoSDKPlugin.getProfilePreferenceStore(selectedProfile));
		yoctoUISetting.setCurrentInput(profileElement);
	}

	public void switchToProjectSpecificProfile()
	{
		YoctoUIElement profileElement = YoctoSDKUtils.getElemFromProjectPreferences(getProject());
		yoctoUISetting.setCurrentInput(profileElement);
	}

	public void switchToSelectedProfile()
	{
		switchProfile(yoctoProfileSetting.getCurrentInput().getSelectedProfile());
	}
}
