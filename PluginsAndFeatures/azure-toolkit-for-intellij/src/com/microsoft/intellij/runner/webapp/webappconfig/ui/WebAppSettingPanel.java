/*
 * Copyright (c) Microsoft Corporation
 *
 * All rights reserved.
 *
 * MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.microsoft.intellij.runner.webapp.webappconfig.ui;

import com.microsoft.intellij.runner.AzureSettingPanel;
import icons.MavenIcons;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionToolbarPosition;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Comparing;
import com.intellij.packaging.artifacts.Artifact;
import com.intellij.ui.AnActionButton;
import com.intellij.ui.HideableDecorator;
import com.intellij.ui.HyperlinkLabel;
import com.intellij.ui.ListCellRendererWrapper;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.table.JBTable;
import com.microsoft.azure.management.appservice.AppServicePlan;
import com.microsoft.azure.management.appservice.JavaVersion;
import com.microsoft.azure.management.appservice.OperatingSystem;
import com.microsoft.azure.management.appservice.PricingTier;
import com.microsoft.azure.management.appservice.WebApp;
import com.microsoft.azure.management.resources.Location;
import com.microsoft.azure.management.resources.ResourceGroup;
import com.microsoft.azure.management.resources.Subscription;
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.core.mvp.model.ResourceEx;
import com.microsoft.azuretools.core.mvp.model.webapp.JdkModel;
import com.microsoft.azuretools.utils.WebAppUtils;
import com.microsoft.intellij.runner.webapp.webappconfig.WebAppConfiguration;
import com.microsoft.intellij.util.MavenRunTaskUtil;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.idea.maven.model.MavenConstants;
import org.jetbrains.idea.maven.project.MavenProject;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;

public class WebAppSettingPanel extends AzureSettingPanel<WebAppConfiguration> implements WebAppDeployMvpView {

    // const
    private static final String RESOURCE_GROUP = "Resource Group";
    private static final String APP_SERVICE_PLAN = "App Service Plan";
    private static final String JAVA = "Java";
    private static final String NOT_APPLICABLE = "N/A";
    private static final String TABLE_LOADING_MESSAGE = "Loading ... ";
    private static final String TABLE_EMPTY_MESSAGE = "No available Web App.";
    private static final String DEFAULT_APP_NAME = "webapp-";
    private static final String DEFAULT_PLAN_NAME = "appsp-";
    private static final String DEFAULT_RGP_NAME = "rg-webapp-";
    private static final String WEB_CONFIG_URL_FORMAT = "https://%s/dev/wwwroot/web.config";
    private static final String NON_WAR_FILE_DEPLOY_HINT = "Please check the web.config file used to deploy this" +
            " non-WAR executable.";
    // presenter
    private final WebAppDeployViewPresenter<WebAppSettingPanel> webAppDeployViewPresenter;
    private final WebAppConfiguration webAppConfiguration;
    // cache variable
    private ResourceEx<WebApp> selectedWebApp = null;
    private List<ResourceEx<WebApp>> cachedWebAppList = null;
    private String lastSelectedSid;
    private String lastSelectedResGrp;
    private String lastSelectedLocation;
    private String lastSelectedPriceTier;

    //widgets
    private JPanel pnlRoot;
    private JPanel pnlExist;
    private JPanel pnlCreate;
    private JPanel pnlWebAppTable;
    private JCheckBox chkToRoot;
    private JRadioButton rdoUseExist;
    private JRadioButton rdoCreateNew;
    private JRadioButton rdoCreateAppServicePlan;
    private JRadioButton rdoUseExistAppServicePlan;
    private JRadioButton rdoCreateResGrp;
    private JRadioButton rdoUseExistResGrp;
    private JTextField txtWebAppName;
    private JTextField txtCreateAppServicePlan;
    private JTextField txtNewResGrp;
    private JTextField txtSelectedWebApp;
    private JComboBox<Subscription> cbSubscription;
    private JComboBox<WebAppUtils.WebContainerMod> cbWebContainer;
    private JComboBox<Location> cbLocation;
    private JComboBox<PricingTier> cbPricing;
    private JComboBox<AppServicePlan> cbExistAppServicePlan;
    private JComboBox<ResourceGroup> cbExistResGrp;
    private JComboBox<JdkModel> cbJdkVersion;
    private JComboBox<Artifact> cbArtifact;
    private JLabel lblLocation;
    private JLabel lblPricing;
    private JLabel lblArtifact;
    private JPanel pnlResourceGroupHolder;
    private JPanel pnlResourceGroup;
    private JPanel pnlAppServicePlanHolder;
    private JPanel pnlAppServicePlan;
    private JPanel pnlJavaHolder;
    private JPanel pnlJava;
    private JLabel lblWebContainer;
    private HyperlinkLabel lblJarDeployHint;
    private JPanel pnlArtifact;
    private JPanel pnlMavenProject;
    private JLabel lblMavenProject;
    private JComboBox<MavenProject> cbMavenProject;
    private JBTable table;
    private AnActionButton btnRefresh;
    /**
     * The setting panel for web app deployment run configuration.
     */
    public WebAppSettingPanel(Project project, @NotNull WebAppConfiguration webAppConfiguration) {
        super(project);
        this.webAppConfiguration = webAppConfiguration;
        this.webAppDeployViewPresenter = new WebAppDeployViewPresenter<>();
        this.webAppDeployViewPresenter.onAttachView(this);

        final ButtonGroup btnGrpForDeploy = new ButtonGroup();
        btnGrpForDeploy.add(rdoUseExist);
        btnGrpForDeploy.add(rdoCreateNew);
        rdoUseExist.addActionListener(e -> toggleDeployPanel(true /*isUsingExisting*/));
        rdoCreateNew.addActionListener(e -> toggleDeployPanel(false /*isUsingExisting*/));
        toggleDeployPanel(true /*showUsingExisting*/);

        final ButtonGroup btnGrpForResGrp = new ButtonGroup();
        btnGrpForResGrp.add(rdoUseExistResGrp);
        btnGrpForResGrp.add(rdoCreateResGrp);
        rdoCreateResGrp.addActionListener(e -> toggleResGrpPanel(true /*isCreatingNew*/));
        rdoUseExistResGrp.addActionListener(e -> toggleResGrpPanel(false /*isCreatingNew*/));

        final ButtonGroup btnGrpForAppServicePlan = new ButtonGroup();
        btnGrpForAppServicePlan.add(rdoUseExistAppServicePlan);
        btnGrpForAppServicePlan.add(rdoCreateAppServicePlan);
        rdoUseExistAppServicePlan.addActionListener(e -> toggleAppServicePlanPanel(false /*isCreatingNew*/));
        rdoCreateAppServicePlan.addActionListener(e -> toggleAppServicePlanPanel(true /*isCreatingNew*/));

        cbExistResGrp.setRenderer(new ListCellRendererWrapper<ResourceGroup>() {
            @Override
            public void customize(JList list, ResourceGroup resourceGroup, int
                    index, boolean isSelected, boolean cellHasFocus) {
                if (resourceGroup != null) {
                    setText(resourceGroup.name());
                }
            }
        });

        cbExistResGrp.addActionListener(e -> {
            ResourceGroup resGrp = (ResourceGroup) cbExistResGrp.getSelectedItem();
            if (resGrp == null) {
                return;
            }
            String selectedGrp = resGrp.name();
            if (!Comparing.equal(lastSelectedResGrp, selectedGrp)) {
                cbExistAppServicePlan.removeAllItems();
                lblLocation.setText(NOT_APPLICABLE);
                lblPricing.setText(NOT_APPLICABLE);
                webAppDeployViewPresenter.onLoadAppServicePlan(lastSelectedSid, selectedGrp);
                lastSelectedResGrp = selectedGrp;
            }
        });

        cbSubscription.setRenderer(new ListCellRendererWrapper<Subscription>() {
            @Override
            public void customize(JList list, Subscription subscription, int
                    index, boolean isSelected, boolean cellHasFocus) {
                if (subscription != null) {
                    setText(subscription.displayName());
                }
            }
        });

        cbSubscription.addActionListener(e -> {
            Subscription subscription = (Subscription) cbSubscription.getSelectedItem();
            if (subscription == null) {
                return;
            }
            String selectedSid = subscription.subscriptionId();
            if (!Comparing.equal(lastSelectedSid, selectedSid)) {
                cbExistResGrp.removeAllItems();
                cbLocation.removeAllItems();
                cbExistAppServicePlan.removeAllItems();
                lblLocation.setText(NOT_APPLICABLE);
                lblPricing.setText(NOT_APPLICABLE);
                webAppDeployViewPresenter.onLoadResourceGroups(selectedSid);
                webAppDeployViewPresenter.onLoadLocation(selectedSid);
                lastSelectedSid = selectedSid;
            }
        });

        cbLocation.setRenderer(new ListCellRendererWrapper<Location>() {
            @Override
            public void customize(JList list, Location location, int
                    index, boolean isSelected, boolean cellHasFocus) {
                if (location != null) {
                    setText(location.displayName());
                }
            }
        });

        cbLocation.addActionListener(e -> {
            Location location = (Location) cbLocation.getSelectedItem();
            if (location != null) {
                lastSelectedLocation = location.name();
            }
        });

        cbExistAppServicePlan.setRenderer(new ListCellRendererWrapper<AppServicePlan>() {
            @Override
            public void customize(JList list, AppServicePlan appServicePlan, int
                    index, boolean isSelected, boolean cellHasFocus) {
                if (appServicePlan != null) {
                    setText(appServicePlan.name());
                }
            }
        });

        cbExistAppServicePlan.addActionListener(e -> {
            AppServicePlan plan = (AppServicePlan) cbExistAppServicePlan.getSelectedItem();
            if (plan != null) {
                lblLocation.setText(plan.regionName());
                lblPricing.setText(plan.pricingTier().toString());
            }
        });

        cbPricing.addActionListener(e -> {
            PricingTier pricingTier = (PricingTier) cbPricing.getSelectedItem();
            if (pricingTier != null) {
                lastSelectedPriceTier = pricingTier.toString();
            }
        });

        // Artifact
        cbArtifact.addActionListener(e -> {
            artifactActionPeformed((Artifact) cbArtifact.getSelectedItem());
        });

        cbArtifact.setRenderer(new ListCellRendererWrapper<Artifact>() {
            @Override
            public void customize(JList list, Artifact artifact, int index, boolean isSelected, boolean cellHasFocus) {
                if (artifact != null) {
                    setIcon(artifact.getArtifactType().getIcon());
                    setText(artifact.getName());
                }
            }
        });

        cbMavenProject.setRenderer(new ListCellRendererWrapper<MavenProject>() {
            @Override
            public void customize(JList jList, MavenProject mavenProject, int i, boolean b, boolean b1) {
                if (mavenProject != null) {
                    setIcon(MavenIcons.MavenProject);
                    setText(mavenProject.toString());
                }
            }
        });

        HideableDecorator resGrpDecorator = new HideableDecorator(pnlResourceGroupHolder,
                RESOURCE_GROUP, true /*adjustWindow*/);
        resGrpDecorator.setContentComponent(pnlResourceGroup);
        resGrpDecorator.setOn(true);

        HideableDecorator appServicePlanDecorator = new HideableDecorator(pnlAppServicePlanHolder,
                APP_SERVICE_PLAN, true /*adjustWindow*/);
        appServicePlanDecorator.setContentComponent(pnlAppServicePlan);
        appServicePlanDecorator.setOn(true);

        HideableDecorator javaDecorator = new HideableDecorator(pnlJavaHolder,
                JAVA, true /*adjustWindow*/);
        javaDecorator.setContentComponent(pnlJava);
        javaDecorator.setOn(true);

        lblJarDeployHint.setHyperlinkText(NON_WAR_FILE_DEPLOY_HINT);
    }

    @Override
    @NotNull
    public String getPanelName() {
        return  "Run On Web App";
    }

    @Override
    @NotNull
    public JPanel getMainPanel() {
        return pnlRoot;
    }

    @Override
    @NotNull
    protected JComboBox<Artifact> getCbArtifact() {
        return cbArtifact;
    }

    @Override
    @NotNull
    protected JLabel getLblArtifact() {
        return lblArtifact;
    }

    @Override
    @NotNull
    protected JComboBox<MavenProject> getCbMavenProject() {
        return cbMavenProject;
    }

    @Override
    @NotNull
    protected JLabel getLblMavenProject() {
        return lblMavenProject;
    }


    /**
     * Function triggered in constructing the panel.
     *
     * @param webAppConfiguration configuration instance
     */
    @Override
    public void resetFromConfig(@NotNull WebAppConfiguration webAppConfiguration) {
        // Default values
        DateFormat df = new SimpleDateFormat("yyMMddHHmmss");
        String date = df.format(new Date());
        if (webAppConfiguration.getWebAppName().isEmpty()) {
            txtWebAppName.setText(DEFAULT_APP_NAME + date);
        } else {
            txtWebAppName.setText(webAppConfiguration.getWebAppName());
        }
        if (webAppConfiguration.getAppServicePlanName().isEmpty()) {
            txtCreateAppServicePlan.setText(DEFAULT_PLAN_NAME + date);
        } else {
            txtCreateAppServicePlan.setText(webAppConfiguration.getAppServicePlanName());
        }
        if (webAppConfiguration.getResourceGroup().isEmpty()) {
            txtNewResGrp.setText(DEFAULT_RGP_NAME + date);
        } else {
            txtNewResGrp.setText(webAppConfiguration.getResourceGroup());
        }

        if (webAppConfiguration.isCreatingNew()) {
            rdoCreateNew.doClick();
            if (webAppConfiguration.isCreatingResGrp()) {
                rdoCreateResGrp.doClick();
            } else {
                rdoUseExistResGrp.doClick();
            }
            if (webAppConfiguration.isCreatingAppServicePlan()) {
                rdoCreateAppServicePlan.doClick();
            } else {
                rdoUseExistAppServicePlan.doClick();
            }
        } else {
            rdoUseExist.doClick();
            chkToRoot.setSelected(webAppConfiguration.isDeployToRoot());
        }
        btnRefresh.setEnabled(false);
        this.webAppDeployViewPresenter.onLoadWebApps();
        this.webAppDeployViewPresenter.onLoadWebContainer();
        this.webAppDeployViewPresenter.onLoadSubscription();
        this.webAppDeployViewPresenter.onLoadPricingTier();
        this.webAppDeployViewPresenter.onLoadJavaVersions();
    }

    /**
     * Function triggered by any content change events.
     *
     * @param webAppConfiguration configuration instance
     */
    @Override
    public void apply(@NotNull WebAppConfiguration webAppConfiguration) {
        webAppConfiguration.setTargetPath(getTargetPath());
        webAppConfiguration.setTargetName(getTargetName());

        String fileType = MavenRunTaskUtil.getFileType(webAppConfiguration.getTargetName());
        if (Comparing.equal(fileType, MavenConstants.TYPE_WAR)) {
            toggleWebContainerSetting(true /*isWar*/);
        } else {
            toggleWebContainerSetting(false /*isWar*/);
        }

        if (rdoUseExist.isSelected()) {
            webAppConfiguration.setWebAppId(selectedWebApp == null ? "" : selectedWebApp.getResource().id());
            webAppConfiguration.setSubscriptionId(selectedWebApp == null ? "" : selectedWebApp.getSubscriptionId());
            webAppConfiguration.setCreatingNew(false);
        } else if (rdoCreateNew.isSelected()) {
            webAppConfiguration.setWebAppName(txtWebAppName.getText());
            webAppConfiguration.setSubscriptionId(lastSelectedSid);
            // resource group
            if (rdoCreateResGrp.isSelected()) {
                webAppConfiguration.setCreatingResGrp(true);
                webAppConfiguration.setResourceGroup(txtNewResGrp.getText());
            } else {
                webAppConfiguration.setCreatingResGrp(false);
                webAppConfiguration.setResourceGroup(lastSelectedResGrp == null ? "" : lastSelectedResGrp);
            }
            // app service plan
            if (rdoCreateAppServicePlan.isSelected()) {
                webAppConfiguration.setCreatingAppServicePlan(true);
                webAppConfiguration.setAppServicePlanName(txtCreateAppServicePlan.getText());

                webAppConfiguration.setRegion(lastSelectedLocation == null ? "" : lastSelectedLocation);
                webAppConfiguration.setPricing(lastSelectedPriceTier == null ? "" : lastSelectedPriceTier);
            } else {
                webAppConfiguration.setCreatingAppServicePlan(false);
                AppServicePlan appServicePlan = (AppServicePlan) cbExistAppServicePlan.getSelectedItem();
                if (appServicePlan != null) {
                    webAppConfiguration.setAppServicePlanId(appServicePlan.id());
                }
            }
            // Java
            JdkModel jdkModel = (JdkModel) cbJdkVersion.getSelectedItem();
            if (jdkModel != null) {
                webAppConfiguration.setJdkVersion(jdkModel.getJavaVersion());
            }
            if (cbWebContainer.isVisible()) {
                WebAppUtils.WebContainerMod container = (WebAppUtils.WebContainerMod) cbWebContainer.getSelectedItem();
                webAppConfiguration.setWebContainer(container == null ? "" : container.getValue());
            } else {
                webAppConfiguration.setWebContainer(WebAppUtils.WebContainerMod.Newest_Tomcat_85.getValue());
            }
            webAppConfiguration.setCreatingNew(true);
        }
        webAppConfiguration.setDeployToRoot(chkToRoot.isSelected());
    }

    @Override
    public void renderWebAppsTable(@NotNull List<ResourceEx<WebApp>> webAppLists) {
        btnRefresh.setEnabled(true);
        table.getEmptyText().setText(TABLE_EMPTY_MESSAGE);
        List<ResourceEx<WebApp>> sortedList = webAppLists.stream()
                .filter(resource -> resource.getResource().javaVersion() != JavaVersion.OFF)
                .sorted((a, b) -> a.getSubscriptionId().compareToIgnoreCase(b.getSubscriptionId()))
                .collect(Collectors.toList());
        cachedWebAppList = sortedList;
        if (sortedList.size() > 0) {
            DefaultTableModel model = (DefaultTableModel) table.getModel();
            model.getDataVector().clear();
            for (int i = 0; i < sortedList.size(); i++) {
                WebApp app = sortedList.get(i).getResource();
                model.addRow(new String[]{
                        app.name(),
                        app.javaVersion().toString(),
                        app.javaContainer() + " " + app.javaContainerVersion(),
                        app.resourceGroupName(),
                });
                if (Comparing.equal(app.id(), webAppConfiguration.getWebAppId())) {
                    table.setRowSelectionInterval(i, i);
                }
            }
        }
    }

    private void toggleDeployPanel(boolean isUsingExisting) {
        pnlExist.setVisible(isUsingExisting);
        pnlCreate.setVisible(!isUsingExisting);
    }

    private void toggleResGrpPanel(boolean isCreatingNew) {
        txtNewResGrp.setEnabled(isCreatingNew);
        cbExistResGrp.setEnabled(!isCreatingNew);
    }

    private void toggleAppServicePlanPanel(boolean isCreatingNew) {
        txtCreateAppServicePlan.setEnabled(isCreatingNew);
        cbLocation.setEnabled(isCreatingNew);
        cbPricing.setEnabled(isCreatingNew);
        cbExistAppServicePlan.setEnabled(!isCreatingNew);
        lblLocation.setEnabled(!isCreatingNew);
        lblPricing.setEnabled(!isCreatingNew);
    }

    private void toggleWebContainerSetting(boolean isWar) {
        lblWebContainer.setVisible(isWar);
        cbWebContainer.setVisible(isWar);
        chkToRoot.setVisible(isWar);
        lblJarDeployHint.setVisible(!isWar && rdoUseExist.isSelected());
    }

    private void resetWidget() {
        btnRefresh.setEnabled(false);
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        model.getDataVector().clear();
        model.fireTableDataChanged();
        table.getEmptyText().setText(TABLE_LOADING_MESSAGE);
        txtSelectedWebApp.setText("");
    }

    private void createUIComponents() {
        DefaultTableModel tableModel = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        tableModel.addColumn("Name");
        tableModel.addColumn("JDK");
        tableModel.addColumn("Web container");
        tableModel.addColumn("Resource group");

        table = new JBTable(tableModel);
        table.getEmptyText().setText(TABLE_LOADING_MESSAGE);
        table.setRowSelectionAllowed(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        table.getSelectionModel().addListSelectionListener(event -> {
            if (event.getValueIsAdjusting()) {
                return;
            }
            int selectedRow = table.getSelectedRow();
            if (cachedWebAppList == null || selectedRow < 0 || selectedRow >= cachedWebAppList.size()) {
                selectedWebApp = null;
                return;
            }
            selectedWebApp = cachedWebAppList.get(selectedRow);
            txtSelectedWebApp.setText(selectedWebApp.toString());
            try {
                String scmSuffix = AuthMethodManager.getInstance().getAzureManager().getScmSuffix();
                lblJarDeployHint.setHyperlinkTarget(String.format(WEB_CONFIG_URL_FORMAT, selectedWebApp.getResource()
                        .name() + scmSuffix));
            } catch (Exception exception) {
                exception.printStackTrace();
            }
        });

        btnRefresh = new AnActionButton("Refresh", AllIcons.Actions.Refresh) {
            @Override
            public void actionPerformed(AnActionEvent anActionEvent) {
                resetWidget();
                webAppDeployViewPresenter.onRefresh();
            }
        };

        ToolbarDecorator tableToolbarDecorator = ToolbarDecorator.createDecorator(table)
                .addExtraActions(btnRefresh).setToolbarPosition(ActionToolbarPosition.TOP);

        pnlWebAppTable = tableToolbarDecorator.createPanel();
    }

    @Override
    public void fillSubscription(@NotNull List<Subscription> subscriptions) {
        cbSubscription.removeAllItems();
        if (subscriptions.size() == 0) {
            lastSelectedSid = null;
            return;
        }
        for (Subscription subscription : subscriptions) {
            cbSubscription.addItem(subscription);
            if (Comparing.equal(subscription.subscriptionId(), webAppConfiguration.getSubscriptionId())) {
                cbSubscription.setSelectedItem(subscription);
            }
        }
    }

    @Override
    public void fillResourceGroup(@NotNull List<ResourceGroup> resourceGroups) {
        cbExistResGrp.removeAllItems();
        if (resourceGroups.size() == 0) {
            lastSelectedResGrp = null;
            return;
        }
        resourceGroups.stream()
                .sorted(Comparator.comparing(ResourceGroup::name))
                .forEach((group) -> {
                    cbExistResGrp.addItem(group);
                    if (Comparing.equal(group.name(), webAppConfiguration.getResourceGroup())) {
                        cbExistResGrp.setSelectedItem(group);
                    }
                });
    }

    @Override
    public void fillAppServicePlan(@NotNull List<AppServicePlan> appServicePlans) {
        cbExistAppServicePlan.removeAllItems();
        if (appServicePlans.size() == 0) {
            lblLocation.setText(NOT_APPLICABLE);
            lblPricing.setText(NOT_APPLICABLE);
            return;
        }
        appServicePlans.stream()
                .filter(item -> Comparing.equal(item.operatingSystem(), OperatingSystem.WINDOWS))
                .sorted(Comparator.comparing(AppServicePlan::name))
                .forEach((plan) -> {
                    cbExistAppServicePlan.addItem(plan);
                    if (Comparing.equal(plan.id(), webAppConfiguration.getAppServicePlanId())) {
                        cbExistAppServicePlan.setSelectedItem(plan);
                    }
                });
    }

    @Override
    public void fillLocation(@NotNull List<Location> locations) {
        cbLocation.removeAllItems();
        if (locations.size() == 0) {
            lastSelectedLocation = null;
            return;
        }
        locations.stream()
                .sorted(Comparator.comparing(Location::displayName))
                .forEach((location) -> {
                    cbLocation.addItem(location);
                    if (Comparing.equal(location.name(), webAppConfiguration.getRegion())) {
                        cbLocation.setSelectedItem(location);
                    }
                });
    }

    @Override
    public void fillPricingTier(@NotNull List<PricingTier> prices) {
        cbPricing.removeAllItems();
        for (PricingTier price : prices) {
            cbPricing.addItem(price);
            if (Comparing.equal(price.toString(), webAppConfiguration.getPricing())) {
                cbPricing.setSelectedItem(price);
            }
        }
    }

    @Override
    public void fillWebContainer(@NotNull List<WebAppUtils.WebContainerMod> webContainers) {
        cbWebContainer.removeAllItems();
        for (WebAppUtils.WebContainerMod container : webContainers) {
            cbWebContainer.addItem(container);
            if (Comparing.equal(container.getValue(), webAppConfiguration.getWebContainer())) {
                cbWebContainer.setSelectedItem(container);
            }
        }
    }

    @Override
    public void fillJdkVersion(@NotNull List<JdkModel> jdks) {
        cbJdkVersion.removeAllItems();
        for (JdkModel jdk : jdks) {
            cbJdkVersion.addItem(jdk);
            if (Comparing.equal(jdk.getJavaVersion(), webAppConfiguration.getJdkVersion())) {
                cbJdkVersion.setSelectedItem(jdk);
            }
        }
    }

    /**
     * Let the presenter release the view. Will be called by:
     * {@link com.microsoft.intellij.runner.webapp.webappconfig.WebAppSettingEditor#disposeEditor()}.
     */
    @Override
    public void disposeEditor() {
        webAppDeployViewPresenter.onDetachView();
    }
}
