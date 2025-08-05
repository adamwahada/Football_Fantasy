import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ShowGameweekMatchesComponent } from './show-gameweek-matches.component';

describe('ShowGameweekMatchesComponent', () => {
  let component: ShowGameweekMatchesComponent;
  let fixture: ComponentFixture<ShowGameweekMatchesComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ShowGameweekMatchesComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ShowGameweekMatchesComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
