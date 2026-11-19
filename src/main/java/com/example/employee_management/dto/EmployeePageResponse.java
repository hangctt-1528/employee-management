package com.example.employee_management.dto;

import java.util.List;

// One page of employees: GET /api/employees and the list / search pages.
// A plain DTO, not Spring's Page, so the JSON shape stays fixed.
public class EmployeePageResponse {

	private List<EmployeeResponse> content;
	private int page; // 0-based
	private int size;
	private long totalElements;
	private int totalPages;
	private boolean first;
	private boolean last;

	public EmployeePageResponse() {
	}

	public EmployeePageResponse(List<EmployeeResponse> content, int page, int size, long totalElements,
			int totalPages, boolean first, boolean last) {
		this.content = content;
		this.page = page;
		this.size = size;
		this.totalElements = totalElements;
		this.totalPages = totalPages;
		this.first = first;
		this.last = last;
	}

	// For "Showing 11-20 of 23" on the web pages. Not getters, so they are not in the JSON.
	public long fromItem() {
		return (long) page * size + 1;
	}

	public long toItem() {
		return (long) page * size + content.size();
	}

	public List<EmployeeResponse> getContent() {
		return content;
	}

	public void setContent(List<EmployeeResponse> content) {
		this.content = content;
	}

	public int getPage() {
		return page;
	}

	public void setPage(int page) {
		this.page = page;
	}

	public int getSize() {
		return size;
	}

	public void setSize(int size) {
		this.size = size;
	}

	public long getTotalElements() {
		return totalElements;
	}

	public void setTotalElements(long totalElements) {
		this.totalElements = totalElements;
	}

	public int getTotalPages() {
		return totalPages;
	}

	public void setTotalPages(int totalPages) {
		this.totalPages = totalPages;
	}

	public boolean isFirst() {
		return first;
	}

	public void setFirst(boolean first) {
		this.first = first;
	}

	public boolean isLast() {
		return last;
	}

	public void setLast(boolean last) {
		this.last = last;
	}

}
