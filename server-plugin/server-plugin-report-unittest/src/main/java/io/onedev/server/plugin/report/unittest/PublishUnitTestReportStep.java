package io.onedev.server.plugin.report.unittest;

import java.io.File;
import java.util.Map;
import java.util.concurrent.Callable;

import javax.annotation.Nullable;

import io.onedev.commons.utils.FileUtils;
import io.onedev.commons.utils.LockUtils;
import io.onedev.commons.utils.TaskLogger;
import io.onedev.server.OneDev;
import io.onedev.server.buildspec.step.PublishReportStep;
import io.onedev.server.model.Build;
import io.onedev.server.model.UnitTestMetric;
import io.onedev.server.persistence.dao.Dao;
import io.onedev.server.web.editable.annotation.Editable;

@Editable
public abstract class PublishUnitTestReportStep extends PublishReportStep {

	private static final long serialVersionUID = 1L;
	
	@Override
	public Map<String, byte[]> run(Build build, File filesDir, TaskLogger logger) {
		File reportDir = new File(build.getReportCategoryDir(UnitTestReport.CATEGORY), getReportName());

		UnitTestReport report = LockUtils.write(build.getReportCategoryLockKey(UnitTestReport.CATEGORY), new Callable<UnitTestReport>() {

			@Override
			public UnitTestReport call() throws Exception {
				UnitTestReport report = createReport(build, filesDir, logger);
				if (report != null) {
					FileUtils.createDir(reportDir);
					report.writeTo(reportDir);
					return report;
				} else {
					return null;
				}
			}
			
		});
		
		if (report != null) {
			UnitTestMetric metric = new UnitTestMetric();
			metric.setBuild(build);
			metric.setReportName(getReportName());
			metric.setTestCaseSuccessRate(report.getTestCaseSuccessRate());
			metric.setTestSuiteSuccessRate(report.getTestSuiteSuccessRate());
			metric.setNumOfTestCases(report.getTestCases().size());
			metric.setNumOfTestSuites(report.getTestSuites().size());
			metric.setTotalTestDuration(report.getTestDuration());
			OneDev.getInstance(Dao.class).persist(metric);
		}	
		
		return null;
	}

	@Nullable
	protected abstract UnitTestReport createReport(Build build, File filesDir, TaskLogger logger);
	
}
